package io.event.ems.controller;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.event.ems.dto.ApiResponse;
import io.event.ems.dto.TicketPurchaseDTO;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.service.impl.TicketPurchaseServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/ticket-purchases")
@RequiredArgsConstructor
@Tag(name = "Ticket Purchase", description = "Ticket Purchase management APIs")
public class TicketPurchaseController {

    private final TicketPurchaseServiceImpl ticketPurchaseService;

    @GetMapping
    @Operation(summary = "Get all ticket purchases", description = "Retrieves all ticket purchases with pagination support.")
    public ResponseEntity<ApiResponse<Page<TicketPurchaseDTO>>> getAllTicketPurchases(@PageableDefault(size = 20) Pageable pageable) {
        Page<TicketPurchaseDTO> ticketPurchases = ticketPurchaseService.getAllTicketPurchases(pageable);
        return ResponseEntity.ok(ApiResponse.success(ticketPurchases));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ticket purchase by ID", description = "Retrieves a ticket purchase by its ID.")
    public ResponseEntity<ApiResponse<TicketPurchaseDTO>> getTicketPurchaseById(@PathVariable UUID id) throws ResourceNotFoundException {
        Optional<TicketPurchaseDTO> ticketPurchase = ticketPurchaseService.getTicketPurchaseById(id);
        return ResponseEntity.ok(ApiResponse.success(ticketPurchase.orElseThrow(() -> new ResourceNotFoundException("Ticket purchase not found with id: " + id))));
    }

    @PostMapping
    @Operation(summary = "Create a new ticket purchase", description = "Creates a new ticket purchase and returns the created ticket purchase.")
    public ResponseEntity<ApiResponse<TicketPurchaseDTO>> createTicketPurchase(@Valid @RequestBody TicketPurchaseDTO ticketPurchaseDTO) throws ResourceNotFoundException {
        TicketPurchaseDTO createdTicketPurchase = ticketPurchaseService.createTicketPurchase(ticketPurchaseDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(createdTicketPurchase));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update ticket purchase", description = "Updates an existing ticket purchase by its ID.")
    public ResponseEntity<ApiResponse<TicketPurchaseDTO>> updateTicketPurchase(@PathVariable UUID id, @Valid @RequestBody TicketPurchaseDTO ticketPurchaseDTO) throws ResourceNotFoundException {
        TicketPurchaseDTO updatedTicketPurchase = ticketPurchaseService.updateTicketPurchase(id, ticketPurchaseDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedTicketPurchase));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete ticket purchase", description = "Deletes a ticket purchase by its ID.")
    public ResponseEntity<ApiResponse<Void>> deleteTicketPurchase(@PathVariable UUID id) throws ResourceNotFoundException {
        ticketPurchaseService.deleteTicketPurchase(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get ticket purchases by user ID", description = "Retrieves ticket purchases by user ID with pagination support.")
    public ResponseEntity<ApiResponse<Page<TicketPurchaseDTO>>> getTicketPurchasesByUserId(@PathVariable UUID userId, @PageableDefault(size = 20) Pageable pageable) {
        Page<TicketPurchaseDTO> ticketPurchases = ticketPurchaseService.getTicketPurchasesByUserId(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(ticketPurchases));
    }

    @GetMapping("/ticket/{ticketId}")
    @Operation(summary = "Get ticket purchases by ticket ID", description = "Retrieves ticket purchases by ticket ID with pagination support.")
    public ResponseEntity<ApiResponse<Page<TicketPurchaseDTO>>> getTicketPurchasesByTicketId(@PathVariable UUID ticketId, @PageableDefault(size = 20) Pageable pageable) {
        Page<TicketPurchaseDTO> ticketPurchases = ticketPurchaseService.getTicketPurchasesByTicketId(ticketId, pageable);
        return ResponseEntity.ok(ApiResponse.success(ticketPurchases));
    }

    @GetMapping("/status/{statusId}")
    @Operation(summary = "Get ticket purchases by status ID", description = "Retrieves ticket purchases by status ID with pagination support.")
    public ResponseEntity<ApiResponse<Page<TicketPurchaseDTO>>> getTicketPurchasesByStatusId(@PathVariable Integer statusId, @PageableDefault(size = 20) Pageable pageable) {
        Page<TicketPurchaseDTO> ticketPurchases = ticketPurchaseService.getTicketPurchasesByStatusId(statusId, pageable);
        return ResponseEntity.ok(ApiResponse.success(ticketPurchases));
    }

}
