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
import io.event.ems.dto.EventParticipantDTO;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.service.EventParticipantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/event-participants")
@RequiredArgsConstructor
@Tag(name = "Event Participant", description = "Event Participant mamagement APIs")
public class EventParticipantController {

    private final EventParticipantService service;

    @GetMapping
    @Operation(summary = "Get all event participants", description = "Retrieves all event participants with pagination support.")
    public ResponseEntity<ApiResponse<Page<EventParticipantDTO>>> getAllEventParticipants(@PageableDefault(size = 20) Pageable pageable){
        Page<EventParticipantDTO> eventParticipant = service.getAllEventParticipant(pageable);
        return ResponseEntity.ok(ApiResponse.success(eventParticipant));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get event participant by ID", description = "Retrieves an event participant by its ID.")
    public ResponseEntity<ApiResponse<EventParticipantDTO>> getEventParticipantById(@PathVariable UUID id){
        Optional<EventParticipantDTO> eventParticipant = service.getEventParticipantById(id);
        return ResponseEntity.ok(ApiResponse.success(eventParticipant.orElseThrow(() -> new ResourceNotFoundException("Event participant not found with id: " + id))));
    }

    @PostMapping
    @Operation(summary = "Create a new event participant", description = "Creates a new event participant and returns the created event participant.")
    public ResponseEntity<ApiResponse<EventParticipantDTO>> createEventParticipant(@Valid @RequestBody EventParticipantDTO eventParticipantDTO) throws ResourceNotFoundException {
        EventParticipantDTO createdEventParticipant = service.createEventParticipant(eventParticipantDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(createdEventParticipant));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update event participant", description = "Updates an existing event participant by its ID.")
    public ResponseEntity<ApiResponse<EventParticipantDTO>> updateEventParticipant(@PathVariable UUID id, @Valid @RequestBody EventParticipantDTO eventParticipantDTO) throws ResourceNotFoundException {
        EventParticipantDTO updateEventParticipant = service.updateEventParticipant(id, eventParticipantDTO);
        return ResponseEntity.ok(ApiResponse.success(updateEventParticipant));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete event participant", description = "Deletes an event participant by its ID.")
    public ResponseEntity<ApiResponse<Void>> deleteEventParticipant(@PathVariable UUID id) throws ResourceNotFoundException {
        service.deleteEventParticipant(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/event/{eventId}")
    @Operation(summary = "Get event participants by event ID", description = "Retrieves event participants by event ID with pagination support.")
    public ResponseEntity<ApiResponse<Page<EventParticipantDTO>>> getEventParticipantByEventId(@PathVariable UUID eventId, @PageableDefault(size = 20) Pageable pageable){
        Page<EventParticipantDTO> eventParticipant = service.getEventParticipantsByEventId(eventId, pageable);
        return ResponseEntity.ok(ApiResponse.success(eventParticipant));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get event participants by user ID", description = "Retrieves event participants by user ID with pagination support.")
    public ResponseEntity<ApiResponse<Page<EventParticipantDTO>>> getEventParticipantsByUserId(@PathVariable UUID userId, @PageableDefault(size = 20) Pageable pageable) {
        Page<EventParticipantDTO> eventParticipants = service.getEventParticipantsByUserId(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(eventParticipants));
    }

    @GetMapping("/status/{statusId}")
    @Operation(summary = "Get event participants by status ID", description = "Retrieves event participants by status ID with pagination support.")
    public ResponseEntity<ApiResponse<Page<EventParticipantDTO>>> getEventParticipantsByStatusId(@PathVariable Integer statusId, @PageableDefault(size = 20) Pageable pageable) {
        Page<EventParticipantDTO> eventParticipants = service.getEventParticipantsByStatusId(statusId, pageable);
        return ResponseEntity.ok(ApiResponse.success(eventParticipants));
    }

}
