package io.event.ems.controller;

import io.event.ems.dto.ApiResponse;
import io.event.ems.dto.SeatMapLayoutDTO;
import io.event.ems.dto.SeatMapSummaryDTO;
import io.event.ems.service.SeatMapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/seat-maps")
@RequiredArgsConstructor
@Tag(name = "Seat Map", description = "Seat map APIs")
public class SeatMapController {

    private final SeatMapService seatMapService;

    @PostMapping("/layout")
    @Operation(summary = "Create a new seat map from a layout", description = "Creates a new seat map from a layout and returns the created seat map ID.")
    public ResponseEntity<ApiResponse<?>> createSeatMapFromLayout(@RequestBody SeatMapLayoutDTO layoutDTO) {
        UUID newMapId = seatMapService.createSeatMapFromLayout(layoutDTO);
        return ResponseEntity.ok(ApiResponse.success("Seat map created successfully", Map.of("seatMapId", newMapId)));
    }

    @GetMapping("/{mapId}/layout")
    @Operation(summary = "Get seat map layout", description = "Retrieves the seat map layout for the given seat map ID.")
    public ResponseEntity<ApiResponse<SeatMapLayoutDTO>> getSeatMapLayout(@PathVariable UUID mapId) {
        SeatMapLayoutDTO layout = seatMapService.getSeatMapLayout(mapId);
        return ResponseEntity.ok(ApiResponse.success(layout));
    }

    @GetMapping
    @Operation(summary = "Get all seat maps", description = "Retrieves all seat maps with pagination support.")
    public ResponseEntity<ApiResponse<Page<SeatMapSummaryDTO>>> getSeatMaps(@RequestParam(required = false) UUID venueId, Pageable pageable) {
        Page<SeatMapSummaryDTO> maps;
        if (venueId != null) {
            maps = seatMapService.getSeatMapsByVenueId(venueId, pageable);
        } else {
            maps = seatMapService.getAllSeatMaps(pageable);
        }

        return ResponseEntity.ok(ApiResponse.success(maps));
    }

    @DeleteMapping("/{mapId}")
    @Operation(summary = "Delete seat map", description = "Deletes the seat map with the given ID.")
    public ResponseEntity<ApiResponse<Void>> deleteSeatMap(@PathVariable UUID mapId) {
        seatMapService.deleteSeatMap(mapId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
