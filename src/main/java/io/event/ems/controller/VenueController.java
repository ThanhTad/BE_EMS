package io.event.ems.controller;

import io.event.ems.dto.ApiResponse;
import io.event.ems.dto.VenueDTO;
import io.event.ems.service.VenueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/venues")
@RequiredArgsConstructor
@Tag(name = "Venue", description = "Venue management APIs")
public class VenueController {

    private final VenueService venueService;

    @PostMapping
    @Operation(summary = "Create a new venue", description = "Creates a new venue and returns the created venue.")
    public ResponseEntity<ApiResponse<VenueDTO>> createVenue(@RequestBody VenueDTO venueDTO) {
        VenueDTO createdVenue = venueService.createVenue(venueDTO);
        return ResponseEntity.status(201).body(ApiResponse.success(createdVenue));
    }

    @GetMapping
    @Operation(summary = "Get all venues", description = "Retrieves all venues with pagination support.")
    public ResponseEntity<ApiResponse<Page<VenueDTO>>> getAllVenues(@PageableDefault(size = 20, sort = "name") Pageable pageable) {
        Page<VenueDTO> venues = venueService.getAllVenues(pageable);
        return ResponseEntity.ok(ApiResponse.success(venues));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get venue by ID", description = "Retrieves a venue by its ID.")
    public ResponseEntity<ApiResponse<VenueDTO>> getVenueById(@PathVariable UUID id) {
        VenueDTO venue = venueService.getVenueById(id);
        return ResponseEntity.ok(ApiResponse.success(venue));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update venue", description = "Updates an existing venue by its ID.")
    public ResponseEntity<ApiResponse<VenueDTO>> updateVenue(@PathVariable UUID id, @RequestBody VenueDTO venueDTO) {
        VenueDTO updatedVenue = venueService.updateVenue(id, venueDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedVenue));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete venue", description = "Deletes a venue by its ID.")
    public ResponseEntity<ApiResponse<Void>> deleteVenue(@PathVariable UUID id) {
        venueService.deleteVenue(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

}
