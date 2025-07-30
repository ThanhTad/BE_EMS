package io.event.ems.controller;

import io.event.ems.dto.ApiResponse;
import io.event.ems.dto.EventCreationDTO;
import io.event.ems.dto.EventResponseDTO;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "APIs for both public browsing and event management")
public class EventController {

    private final EventService eventService;

    // =================================================================
    // ADMIN & ORGANIZER ENDPOINTS
    // =================================================================

    @GetMapping("/admin") // URL: /api/v1/events/admin
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    @Operation(summary = "[ADMIN] Get all events for management view",
            description = "**Requires ADMIN or ORGANIZER role.** Creates a new event and returns the created event.")
    public ResponseEntity<ApiResponse<Page<EventResponseDTO>>> getAllEventsForManagement(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        // Service sẽ có logic để phân biệt:
        // - Nếu là ADMIN -> trả về tất cả.
        // - Nếu là ORGANIZER -> chỉ trả về các event do họ tạo.
        Page<EventResponseDTO> events = eventService.getAllEventsForManagement(pageable);

        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    @Operation(
            summary = "Create a new event",
            description = "**Requires ADMIN or ORGANIZER role.** Creates a new event and returns the created event."
    )
    public ResponseEntity<ApiResponse<EventResponseDTO>> createEvent(@Valid @RequestBody EventCreationDTO eventCreationDTO) {
        EventResponseDTO createdEvent = eventService.createEvent(eventCreationDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(createdEvent));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    @Operation(
            summary = "Update an event",
            description = "**Requires ADMIN or ORGANIZER role.** Updates an existing event by its ID."
    )
    public ResponseEntity<ApiResponse<EventResponseDTO>> updateEvent(@PathVariable UUID id,
                                                                     @Valid @RequestBody EventCreationDTO eventRequestDTO) throws ResourceNotFoundException {
        EventResponseDTO updatedEvent = eventService.updateEvent(id, eventRequestDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedEvent));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    @Operation(
            summary = "Delete an event",
            description = "**Requires ADMIN or ORGANIZER role.** Deletes an event by its ID."
    )
    public ResponseEntity<ApiResponse<Void>> deleteEvent(@PathVariable UUID id) {
        eventService.deleteEvent(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Approve a pending event",
            description = "**Requires ADMIN role.** Approves an event, making it public if applicable."
    )
    public ResponseEntity<ApiResponse<EventResponseDTO>> approveEvent(@PathVariable UUID id) {
        EventResponseDTO approvedEvent = eventService.approveEvent(id);
        return ResponseEntity.ok(ApiResponse.success(approvedEvent));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Reject a pending event",
            description = "**Requires ADMIN role.** Rejects a pending event."
    )
    public ResponseEntity<ApiResponse<EventResponseDTO>> rejectEvent(@PathVariable UUID id) {
        EventResponseDTO rejectedEvent = eventService.rejectEvent(id);
        return ResponseEntity.ok(ApiResponse.success(rejectedEvent));
    }

    // =================================================================
    // PUBLIC & ADMIN SHARED ENDPOINTS
    // Endpoint /search có thể được dùng bởi cả hai, nhưng với các filter khác nhau
    // Chúng ta sẽ dùng một endpoint và để service xử lý. Swagger sẽ mô tả cả hai trường hợp.
    // =================================================================

    @GetMapping("/search")
    @Operation(
            summary = "Advanced event search (Public & Admin)",
            description = "Searches for events with multiple filters.<br>" +
                    "**For Public users:** Only searches approved and public events.<br>" +
                    "**For Admin/Organizer:** Can use additional filters like `statusId` and `isPublic` to search all events."
    )
    public ResponseEntity<ApiResponse<Page<EventResponseDTO>>> searchEvents(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<UUID> categoryIds,
            @RequestParam(required = false) Integer statusId, // Sẽ là null nếu public user gọi
            @RequestParam(required = false) Boolean isPublic, // Sẽ là null nếu public user gọi
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(sort = "startDate") Pageable pageable) {

        // Service của bạn cần có logic phân quyền bên trong để xử lý các filter admin
        Page<EventResponseDTO> events = eventService.searchEventsWithFilters(
                keyword, categoryIds, statusId, isPublic, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(events));
    }


    // =================================================================
    // PUBLIC ENDPOINTS
    // =================================================================

    @GetMapping("/public")
    @Operation(
            summary = "Get all public events",
            description = "Retrieves all approved and public events with pagination support. No authentication required."
    )
    public ResponseEntity<ApiResponse<Page<EventResponseDTO>>> getPublicEvents(
            @PageableDefault(size = 6, sort = "startDate") Pageable pageable) {
        Page<EventResponseDTO> events = eventService.getPublicEvents(pageable);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @GetMapping("/slug/{slug}")
    @Operation(
            summary = "Get a public event by slug",
            description = "Retrieves a single, publicly visible event by its unique slug. No authentication required."
    )
    public ResponseEntity<ApiResponse<EventResponseDTO>> getEventBySlug(@PathVariable String slug) {
        Optional<EventResponseDTO> event = eventService.getEventBySlug(slug);
        return event.map(dto -> ResponseEntity.ok(ApiResponse.success(dto)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(HttpStatus.NOT_FOUND, "Event not found with slug: " + slug)));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(
            summary = "Get public events by category",
            description = "Retrieves public events belonging to a specific category. No authentication required."
    )
    public ResponseEntity<ApiResponse<Page<EventResponseDTO>>> getEventsByCategoryId(@PathVariable UUID categoryId, @PageableDefault(size = 6) Pageable pageable) {
        Page<EventResponseDTO> events = eventService.findByCategories_Id(categoryId, pageable);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

}
