package io.event.ems.controller;

import io.event.ems.dto.ApiResponse;
import io.event.ems.dto.TicketRequestDTO;
import io.event.ems.dto.TicketResponseDTO;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/events/{eventId}/tickets")
@RequiredArgsConstructor
@Tag(name = "Ticket", description = "Ticket management APIs")
@PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
public class TicketController {

    private final TicketService ticketService;

    @GetMapping
    @Operation(
            summary = "Get all tickets for an event (Admin View)",
            description = "**Requires ADMIN or ORGANIZER role.** Retrieves all tickets for a specific event, including inactive or sold-out ones."
    )
    public ResponseEntity<ApiResponse<Page<TicketResponseDTO>>> getTicketsForEvent(
            @PathVariable UUID eventId,
            @PageableDefault() Pageable pageable) {
        Page<TicketResponseDTO> tickets = ticketService.getTicketsForEvent(eventId, pageable);
        return ResponseEntity.ok(ApiResponse.success(tickets));
    }

    @GetMapping("/{ticketId}")
    @Operation(
            summary = "Get a ticket by ID",
            description = "**Requires ADMIN or ORGANIZER role.** Retrieves a specific ticket by its ID."
    )
    public ResponseEntity<ApiResponse<TicketResponseDTO>> getTicketById(
            @PathVariable UUID eventId,
            @PathVariable UUID ticketId) throws ResourceNotFoundException {
        Optional<TicketResponseDTO> ticket = ticketService.getTicketById(eventId, ticketId);
        return ticket.map(dto -> ResponseEntity.ok(ApiResponse.success(dto)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(HttpStatus.NOT_FOUND, "Ticket not found with id: " + ticketId)));
    }

    @PostMapping
    @Operation(
            summary = "Create a new ticket for an event",
            description = "**Requires ADMIN or ORGANIZER role.** Creates a new ticket type for the specified event."
    )
    public ResponseEntity<ApiResponse<TicketResponseDTO>> createTicketForEvent(
            @PathVariable UUID eventId,
            @Valid @RequestBody TicketRequestDTO ticketRequestDTO) {
        TicketResponseDTO createdTicket = ticketService.createTicketForEvent(eventId, ticketRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(createdTicket));
    }

    @PutMapping("/{ticketId}")
    @Operation(
            summary = "Update a ticket",
            description = "**Requires ADMIN or ORGANIZER role.** Updates an existing ticket by its ID."
    )
    public ResponseEntity<ApiResponse<TicketResponseDTO>> updateTicket(
            @PathVariable UUID eventId,
            @PathVariable UUID ticketId,
            @Valid @RequestBody TicketRequestDTO ticketRequestDTO) {
        TicketResponseDTO updatedTicket = ticketService.updateTicket(eventId, ticketId, ticketRequestDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedTicket));
    }

    @DeleteMapping("/{ticketId}")
    @Operation(
            summary = "Delete a ticket",
            description = "**Requires ADMIN or ORGANIZER role.** Deletes a ticket by its ID."
    )
    public ResponseEntity<ApiResponse<Void>> deleteTicket(
            @PathVariable UUID eventId,
            @PathVariable UUID ticketId) {
        ticketService.deleteTicket(eventId, ticketId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
