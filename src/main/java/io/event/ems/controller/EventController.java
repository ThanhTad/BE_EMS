package io.event.ems.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.event.ems.dto.ApiResponse;
import io.event.ems.dto.EventRequestDTO;
import io.event.ems.dto.EventResponseDTO;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Event", description = "Event management APIs")
public class EventController {

    private final EventService eventService;

    @PostMapping
    @Operation(summary = "Create a new event", description = "Creates a new event and returns the created event.")
    public ResponseEntity<ApiResponse<EventResponseDTO>> createEvent(@RequestBody EventRequestDTO eventRequestDTO) {
        EventResponseDTO createEvent = eventService.createEvent(eventRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(createEvent));
    }

    @GetMapping
    @Operation(summary = "Get all events", description = "Retrieves all event with pagination support.")
    public ResponseEntity<ApiResponse<Page<EventResponseDTO>>> getAllEvents(
            @PageableDefault(size = 6) Pageable pageable) {

        Page<EventResponseDTO> events = eventService.getAllEvents(pageable);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get event by Id", description = "Retrieves an eventby its Id.")
    public ResponseEntity<ApiResponse<EventResponseDTO>> getEventById(@PathVariable UUID id)
            throws ResourceNotFoundException {
        Optional<EventResponseDTO> event = eventService.getEventById(id);
        return event.map(dto -> new ResponseEntity<>(ApiResponse.success(dto), HttpStatus.OK))
                .orElse(new ResponseEntity<>(ApiResponse.error(HttpStatus.NOT_FOUND, "Event not found with id: " + id),
                        HttpStatus.NOT_FOUND));
    }

    @GetMapping("/search")
    @Operation(summary = "Search events", description = "Searches events by keyword with pagination support.")
    public ResponseEntity<ApiResponse<Page<EventResponseDTO>>> searchEvents(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 6) Pageable pageable) {
        Page<EventResponseDTO> events = eventService.searchEvents(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update event", description = "Updates an existing event by its Id")
    public ResponseEntity<ApiResponse<EventResponseDTO>> updateEvent(@PathVariable UUID id,
            @RequestBody EventRequestDTO eventRequestDTO) throws ResourceNotFoundException {

        EventResponseDTO updatedEvent = eventService.updateEvent(id, eventRequestDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedEvent));

    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete event", description = "Deletes an event by its Id")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(@PathVariable UUID id) {
        eventService.deleteEvent(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/creator/{creatorId}")
    @Operation(summary = "Get events by creator ID", description = "Retrieves events created by a specific user with pagination support.")
    public ResponseEntity<ApiResponse<Page<EventResponseDTO>>> getEventsByCreatorId(
            @PathVariable UUID creatorId,
            @PageableDefault(size = 6) Pageable pageable) {
        Page<EventResponseDTO> events = eventService.getEventByCreatorId(creatorId, pageable);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get events by category ID", description = "Retrieves events belonging to a specific category with pagination support.")
    public ResponseEntity<ApiResponse<Page<EventResponseDTO>>> getEventsByCategoryId(
            @PathVariable UUID categoryId,
            @PageableDefault(size = 6) Pageable pageable) {
        Page<EventResponseDTO> events = eventService.findByCategories_Id(categoryId, pageable);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @GetMapping("/status/{statusId}")
    @Operation(summary = "Get events by status ID", description = "Retrieves events with a specific status with pagination support.")
    public ResponseEntity<ApiResponse<Page<EventResponseDTO>>> getEventsByStatusId(
            @PathVariable Integer statusId,
            @PageableDefault(size = 6) Pageable pageable) {
        Page<EventResponseDTO> events = eventService.getEventByStatusId(statusId, pageable);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @GetMapping("/date")
    @Operation(summary = "Get event by start date range", description = "Retrieves events within a specific start date range with pagination support.")
    public ResponseEntity<ApiResponse<Page<EventResponseDTO>>> getEventsByStartDateBetween(
            @RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime end,
            @PageableDefault(size = 6) Pageable pageable) {
        Page<EventResponseDTO> events = eventService.getEventByStartDateBetween(start, end, pageable);
        return ResponseEntity.ok(ApiResponse.success(events));
    }
}
