package io.event.ems.controller;

import io.event.ems.dto.ApiResponse;
import io.event.ems.dto.SeatMapDetailDTO;
import io.event.ems.dto.SeatMapListItemDTO;
import io.event.ems.dto.UpdateSeatMapRequestDTO;
import io.event.ems.service.SeatMapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/venues/{venueId}/seat-maps")
@RequiredArgsConstructor
@Tag(name = "Admin: Seat Map", description = "Seat Map Management APIs")
@PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
public class SeatMapController {

    private final SeatMapService seatMapService;

    @GetMapping
    @Operation(summary = "Get all seat maps for a venue", description = "Retrieves all seat maps for a specific venue.")
    public ResponseEntity<ApiResponse<List<SeatMapListItemDTO>>> getSeatMapsForVenue(@PathVariable UUID venueId) {
        List<SeatMapListItemDTO> seatMaps = seatMapService.getSeatMapsByVenue(venueId);
        return ResponseEntity.ok(ApiResponse.success(seatMaps));
    }

    @PostMapping
    @Operation(summary = "Create a new seat map for a venue", description = "Creates a new seat map for a specific venue and returns the created seat map.")
    public ResponseEntity<ApiResponse<SeatMapDetailDTO>> createSeatMap(
            @PathVariable UUID venueId,
            @Valid @RequestBody UpdateSeatMapRequestDTO dto) {
        SeatMapDetailDTO createdSeatMap = seatMapService.createSeatMap(venueId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(createdSeatMap));
    }

    // Các endpoint cho một seat map cụ thể
    @RestController
    @RequestMapping("/api/v1/admin/seat-maps")
    @RequiredArgsConstructor
    @Tag(name = "Admin: Seat Map", description = "Seat Map Management APIs")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public static class SingleSeatMapController {

        private final SeatMapService seatMapService;

        @GetMapping("/{seatMapId}/details")
        @Operation(summary = "Get full details of a single seat map", description = "Retrieves full details of a single seat map by its ID.")
        public ResponseEntity<ApiResponse<SeatMapDetailDTO>> getSeatMapDetails(@PathVariable UUID seatMapId) {
            return ResponseEntity.ok(ApiResponse.success(seatMapService.getSeatMapDetails(seatMapId)));
        }

        @PutMapping("/{seatMapId}")
        @Operation(summary = "Update an existing seat map", description = "Updates an existing seat map by its ID and returns the updated seat map.")
        public ResponseEntity<ApiResponse<SeatMapDetailDTO>> updateSeatMap(
                @PathVariable UUID seatMapId,
                @Valid @RequestBody UpdateSeatMapRequestDTO dto) {
            SeatMapDetailDTO updatedSeatMap = seatMapService.updateSeatMap(seatMapId, dto);
            return ResponseEntity.ok(ApiResponse.success(updatedSeatMap));
        }

        @DeleteMapping("/{seatMapId}")
        @Operation(summary = "Delete a seat map", description = "Deletes a seat map by its ID")
        public ResponseEntity<ApiResponse<Void>> deleteSeatMap(@PathVariable UUID seatMapId) {
            seatMapService.deleteSeatMap(seatMapId);
            return ResponseEntity.ok(ApiResponse.success(null));
        }
    }
}
