package io.event.ems.controller;

import io.event.ems.dto.ApiResponse;
import io.event.ems.dto.TicketRequestDTO;
import io.event.ems.dto.TicketResponseDTO;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.service.impl.TicketServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events/{eventId}/tickets")
@RequiredArgsConstructor
@Tag(name = "Ticket", description = "Ticket management APIs")
public class TicketController {

    private final TicketServiceImpl ticketServiceImpl;

    @GetMapping()
    @Operation(summary = "Get tickets for event", description = "Retrieves tickets for event with pagination support.")
    public ResponseEntity<ApiResponse<Page<TicketResponseDTO>>> getTicketsForEvent(@PathVariable UUID eventId, @PageableDefault(size = 6) Pageable pageable) {
        Page<TicketResponseDTO> tickets = ticketServiceImpl.getTicketsForEvent(eventId, pageable);
        return ResponseEntity.ok(ApiResponse.success(tickets));
    }

    @GetMapping("/{ticketId}")
    @Operation(summary = "Get ticket by ID", description = "Retrieves a ticket by its ID.")
    public ResponseEntity<ApiResponse<TicketResponseDTO>> getTicketById(@PathVariable UUID eventId,
                                                                        @PathVariable UUID ticketId) throws ResourceNotFoundException {
        Optional<TicketResponseDTO> ticket = ticketServiceImpl.getTicketById(eventId, ticketId);
        return ResponseEntity.ok(ApiResponse.success(ticket.orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId + " for event id: " + eventId + "."))));
    }

    @PostMapping
    @Operation(summary = "Create a new ticket", description = "Creates a new ticket and returns the created ticket.")
    public ResponseEntity<ApiResponse<TicketResponseDTO>> createTicketForEvent(@PathVariable UUID eventId, @RequestBody TicketRequestDTO ticketRequestDTO) {
        TicketResponseDTO createdTicket = ticketServiceImpl.createTicketForEvent(eventId, ticketRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(createdTicket));
    }

    @PutMapping("/{ticketId}")
    @Operation(summary = "Update ticket", description = "Updates an existing ticket by its ID.")
    public ResponseEntity<ApiResponse<TicketResponseDTO>> updateTicket(@PathVariable UUID eventId,
                                                                       @PathVariable UUID ticketId, @RequestBody TicketRequestDTO ticketRequestDTO) {
        TicketResponseDTO updatedTicket = ticketServiceImpl.updateTicket(eventId, ticketId, ticketRequestDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedTicket));
    }

    @DeleteMapping("/{ticketId}")
    @Operation(summary = "Delete ticket", description = "Deletes a ticket by its ID.")
    public ResponseEntity<ApiResponse<Void>> deleteTicket(@PathVariable UUID eventId, @PathVariable UUID ticketId) {
        ticketServiceImpl.deleteTicket(eventId, ticketId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
