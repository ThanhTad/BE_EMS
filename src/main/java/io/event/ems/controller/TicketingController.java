package io.event.ems.controller;

import io.event.ems.dto.*;
import io.event.ems.exception.UnauthorizedException;
import io.event.ems.security.CustomUserDetails;
import io.event.ems.service.EventTicketingQueryService;
import io.event.ems.service.TicketHoldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/ticketing")
@RequiredArgsConstructor
@Tag(name = "Ticketing", description = "Ticketing APIs")
public class TicketingController {

    private final EventTicketingQueryService queryService;
    private final TicketHoldService holdService;

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

    @GetMapping("/hold/{holdId}/details")
    @Operation(summary = "Get hold details", description = "Get details of an existing hold.")
    public ResponseEntity<ApiResponse<HoldDetailsResponseDTO>> getHoldDetails(
            @PathVariable UUID holdId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        UUID userId = Optional.ofNullable(currentUser)
                .orElseThrow(() -> new UnauthorizedException("User principal not found."))
                .getId();
        HoldDetailsResponseDTO
                holdDetailsResponseDTO = holdService.getHoldDetails(holdId, userId);
        return ResponseEntity.ok(ApiResponse.success(holdDetailsResponseDTO));

    }

    @PostMapping("/hold/{holdId}/release")
    @Operation(summary = "Release hold", description = "Release hold and finalize transaction.")
    public ResponseEntity<ApiResponse<String>> releaseTickets(
            @PathVariable UUID holdId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        UUID userId = Optional.ofNullable(currentUser)
                .orElseThrow(() -> new UnauthorizedException("User principal not found."))
                .getId();
        holdService.releaseHold(holdId, userId);
        return ResponseEntity.ok(ApiResponse.success("Hold released successfully."));
    }
}
