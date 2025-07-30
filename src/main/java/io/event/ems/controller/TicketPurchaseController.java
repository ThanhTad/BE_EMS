package io.event.ems.controller;

import io.event.ems.dto.ApiResponse;
import io.event.ems.dto.PurchaseDetailDTO;
import io.event.ems.dto.PurchaseListItemDTO;
import io.event.ems.security.CustomUserDetails;
import io.event.ems.service.TicketPurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Ticket Purchase", description = "Ticket Purchase APIs")
public class TicketPurchaseController {

    private final TicketPurchaseService ticketPurchaseService;

    @GetMapping("/admin/purchases")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all purchases", description = "Retrieves all purchases with pagination support.")
    public ResponseEntity<ApiResponse<Page<PurchaseListItemDTO>>> getAllPurchases(
            @PageableDefault(size = 20, sort = "purchaseDate") Pageable pageable
    ) {
        Page<PurchaseListItemDTO> purchases = ticketPurchaseService.getAllPurchases(pageable);
        return ResponseEntity.ok(ApiResponse.success(purchases));
    }

    @GetMapping("/admin/purchases/{purchaseId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get purchase details by ID", description = "Retrieves a purchase details by its ID.")
    public ResponseEntity<ApiResponse<PurchaseDetailDTO>> getPurchaseDetailsByAdmin(
            @PathVariable UUID purchaseId
    ) {
        PurchaseDetailDTO purchaseDetail = ticketPurchaseService.getPurchaseDetailsById(purchaseId);
        return ResponseEntity.ok(ApiResponse.success(purchaseDetail));
    }

    @GetMapping("/users/me/purchases")
    @Operation(summary = "Get my purchases", description = "Retrieves all purchases by current user with pagination support.")
    public ResponseEntity<ApiResponse<Page<PurchaseListItemDTO>>> getMyPurchases(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PageableDefault(size = 20, sort = "purchaseDate") Pageable pageable
    ) {
        Page<PurchaseListItemDTO> purchases = ticketPurchaseService.getPurchasesByUserId(currentUser.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(purchases));
    }

    @GetMapping("users/me/purchases/{purchaseId}")
    @Operation(summary = "Get purchase details by ID", description = "Retrieves a purchase details by its ID.")
    public ResponseEntity<ApiResponse<PurchaseDetailDTO>> getPurchaseDetailsByMe(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID purchaseId
    ) {
        PurchaseDetailDTO purchaseDetail = ticketPurchaseService.getPurchaseDetailsByIdForUser(purchaseId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(purchaseDetail));
    }
}
