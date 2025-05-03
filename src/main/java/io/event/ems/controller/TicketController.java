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
import io.event.ems.dto.TicketDTO;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.service.impl.TicketServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Tag(name = "Ticket", description = "Ticket management APIs")
public class TicketController {

    private final TicketServiceImpl ticketServiceImpl;

    @GetMapping()
    @Operation(summary = "Get all tickets", description = "Retrieves all tickets with pagination support.")
    public ResponseEntity<ApiResponse<Page<TicketDTO>>> getAllTickets(@PageableDefault(size = 20) Pageable pageable){
        Page<TicketDTO> tickets = ticketServiceImpl.getAllTicket(pageable);
        return ResponseEntity.ok(ApiResponse.success(tickets));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ticket by ID", description = "Retrieves a ticket by its ID.")
    public ResponseEntity<ApiResponse<TicketDTO>> getTicketById(@PathVariable UUID id) throws ResourceNotFoundException{
        Optional<TicketDTO> ticket = ticketServiceImpl.getTicketById(id);
        return ResponseEntity.ok(ApiResponse.success(ticket.orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id))));
    }

    @PostMapping
    @Operation(summary = "Create a new ticket", description = "Creates a new ticket and returns the created ticket.")
    public ResponseEntity<ApiResponse<TicketDTO>> createTicket(@RequestBody TicketDTO ticketDTO){
        TicketDTO createdTicket = ticketServiceImpl.createTicket(ticketDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(createdTicket));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update ticket", description = "Updates an existing ticket by its ID.")
    public ResponseEntity<ApiResponse<TicketDTO>> updateTicket(@PathVariable UUID id, @RequestBody TicketDTO ticketDTO){
        TicketDTO updatedTicket = ticketServiceImpl.updateTicket(id, ticketDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedTicket));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete ticket", description = "Deletes a ticket by its ID.")
    public ResponseEntity<ApiResponse<Void>> deleteTicket(@PathVariable UUID id){
        ticketServiceImpl.deleteTicket(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/event/{eventId}")
    @Operation(summary = "Get tickets by event ID", description = "Retrieves tickets by event ID with pagination support.")
    public ResponseEntity<ApiResponse<Page<TicketDTO>>> getTicketByEventId(@PathVariable UUID eventId, 
    @PageableDefault(size = 20) Pageable pageable){

        Page<TicketDTO> tickets = ticketServiceImpl.getTicketByEventId(eventId, pageable);
        return ResponseEntity.ok(ApiResponse.success(tickets));

    }

    @GetMapping("/status/{statusId}")
    @Operation(summary = "Get tickets by status ID", description = "Retrieves tickets by status ID with pagination support.")
    public ResponseEntity<ApiResponse<Page<TicketDTO>>> getTicketByStatusId(@PathVariable Integer statusId, 
    @PageableDefault(size = 20) Pageable pageable){

        Page<TicketDTO> tickets = ticketServiceImpl.getTicketByStatusId(statusId, pageable);
        return ResponseEntity.ok(ApiResponse.success(tickets));

    }

}
