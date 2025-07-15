package io.event.ems.controller;

import io.event.ems.dto.*;
import io.event.ems.exception.UnauthorizedException;
import io.event.ems.model.HoldData;
import io.event.ems.security.CustomUserDetails;
import io.event.ems.service.EventTicketingQueryService;
import io.event.ems.service.OrderProcessingService;
import io.event.ems.service.TicketHoldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ticketing")
@RequiredArgsConstructor
@Tag(name = "Ticketing", description = "Ticketing APIs")
public class TicketingController {

    private final EventTicketingQueryService queryService;
    private final TicketHoldService holdService;
    private final OrderProcessingService orderProcessingService;

    @GetMapping("/events/slug/{slug}")
    @Operation(summary = "Get event ticketing by slug", description = "Get event ticketing by slug.")
    public ResponseEntity<ApiResponse<EventTicketingResponseDTO>> getEventTicketingBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(queryService.getEventTicketingBySlug(slug)));
    }

    @PostMapping("/events/{eventId}/hold")
    @Operation(summary = "Create hold", description = "Create hold and validate tickets.")
    public ResponseEntity<ApiResponse<HoldResponseDTO
            >> holdTickets(
            @PathVariable UUID eventId,
            @RequestBody TicketHoldRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        UUID userId = Optional.ofNullable(currentUser)
                .orElseThrow(() -> new UnauthorizedException("User principal not found."))
                .getId();
        HoldResponseDTO response = holdService.createAndValidateHold(eventId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/release")
    @Operation(summary = "Release hold", description = "Release hold and finalize transaction.")
    public ResponseEntity<ApiResponse<String>> releaseTickets(
            @RequestParam UUID holdId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        UUID userId = Optional.ofNullable(currentUser)
                .orElseThrow(() -> new UnauthorizedException("User principal not found."))
                .getId();
        holdService.releaseHold(holdId, userId);
        return ResponseEntity.ok(ApiResponse.success("Hold released successfully."));
    }

    @PostMapping("/checkout")
    @Operation(summary = "Checkout hold", description = "Checkout hold and finalize transaction.")
    public ResponseEntity<ApiResponse<TicketPurchaseConfirmationDTO>> checkout(
            @RequestParam UUID holdId,
            @RequestBody PaymentDetailsDTO paymentDetails,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        UUID userId = Optional.ofNullable(currentUser)
                .orElseThrow(() -> new UnauthorizedException("User principal not found."))
                .getId();
        HoldData holdData = null;

        try {
            // Bước 1: Lấy và xóa hold khỏi Redis một cách an toàn.
            holdData = holdService.getAndFinalizeHold(holdId, userId);

            // Bước 2: Bắt đầu transaction để ghi vào DB.
            TicketPurchaseConfirmationDTO confirmation = orderProcessingService.finalizePurchase(holdData, paymentDetails);

            return ResponseEntity.ok(ApiResponse.success(confirmation));
        } catch (Exception e) {
            // Bước 3 (quan trọng): Nếu checkout lỗi, phải cố gắng nhả lại tài nguyên đã giữ.
            if (holdData != null) {
                // Service này sẽ cộng trả lại vé GA/Zoned vào DB, hoặc xóa ghế khỏi set trong Redis.
                holdService.releaseResourcesForFailedCheckout(holdData);
            }
            throw e;
        }

    }
}
