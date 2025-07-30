package io.event.ems.controller;

import io.event.ems.dto.ApiResponse;
import io.event.ems.dto.VenueDTO;
import io.event.ems.dto.VenueRequestDTO;
import io.event.ems.service.VenueService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/venues")
@RequiredArgsConstructor
@Tag(name = "Admin: Venue", description = "Venue Management APIs")
@PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
public class VenueController {

    private final VenueService venueService;

    @GetMapping
    @Operation(summary = "Get all venues with pagination and search", description = "Retrieves all venues with pagination support and search support.")
    public ResponseEntity<ApiResponse<Page<VenueDTO>>> getAllVenues(
            @RequestParam(required = false) String keyword,
            @PageableDefault(sort = "name") Pageable pageable) {
        Page<VenueDTO> venues = venueService.getAllVenues(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(venues));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single venue by ID", description = "Retrieves a single venue by its ID.")
    public ResponseEntity<ApiResponse<VenueDTO>> getVenueById(@PathVariable UUID id) {
        VenueDTO venue = venueService.getVenueById(id);
        return ResponseEntity.ok(ApiResponse.success(venue));
    }

    @PostMapping
    @Operation(summary = "Create a new venue", description = "Creates a new venue and returns the created venue.")
    public ResponseEntity<ApiResponse<VenueDTO>> createVenue(@Valid @RequestBody VenueRequestDTO dto) {
        VenueDTO createdVenue = venueService.createVenue(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(createdVenue));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing venue", description = "Updates an existing venue by its ID.")
    public ResponseEntity<ApiResponse<VenueDTO>> updateVenue(@PathVariable UUID id, @Valid @RequestBody VenueRequestDTO dto) {
        VenueDTO updatedVenue = venueService.updateVenue(id, dto);
        return ResponseEntity.ok(ApiResponse.success(updatedVenue));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a venue", description = "Deletes a venue by its ID.")
    public ResponseEntity<ApiResponse<Void>> deleteVenue(@PathVariable UUID id) {
        venueService.deleteVenue(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
