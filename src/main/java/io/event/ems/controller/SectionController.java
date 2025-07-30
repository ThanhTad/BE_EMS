package io.event.ems.controller;

import io.event.ems.dto.ApiResponse;
import io.event.ems.dto.SectionDetailDTO;
import io.event.ems.dto.SectionRequestDTO;
import io.event.ems.service.SectionService;
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
@RequiredArgsConstructor
@Tag(name = "Admin: Section (Zone)", description = "APIs for managing sections/zones within a Seat Map")
@PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
public class SectionController {

    private final SectionService sectionService;

    @GetMapping("/api/v1/admin/seat-maps/{seatMapId}/sections")
    @Operation(summary = "Get all sections/zones for a specific seat map", description = "Retrieves all sections/zones for a specific seat map.")
    public ResponseEntity<ApiResponse<List<SectionDetailDTO>>> getSectionsBySeatMap(@PathVariable UUID seatMapId) {
        return ResponseEntity.ok(ApiResponse.success(sectionService.getSectionsBySeatMap(seatMapId)));
    }

    @PostMapping("/api/v1/admin/seat-maps/{seatMapId}/sections")
    @Operation(summary = "Create a new section/zone for a seat map", description = "Creates a new section/zone for a specific seat map and returns the created section/zone.")
    public ResponseEntity<ApiResponse<SectionDetailDTO>> createSection(
            @PathVariable UUID seatMapId,
            @Valid @RequestBody SectionRequestDTO dto) {
        SectionDetailDTO createdSection = sectionService.createSection(seatMapId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(createdSection));
    }

    @GetMapping("/api/v1/admin/sections/{sectionId}")
    @Operation(summary = "Get a single section/zone by its ID", description = "Retrieves a single section/zone by its ID.")
    public ResponseEntity<ApiResponse<SectionDetailDTO>> getSectionById(@PathVariable UUID sectionId) {
        return ResponseEntity.ok(ApiResponse.success(sectionService.getSectionById(sectionId)));
    }

    @PutMapping("/api/v1/admin/sections/{sectionId}")
    @Operation(summary = "Update an existing section/zone", description = "Updates an existing section/zone by its ID and returns the updated section/zone.")
    public ResponseEntity<ApiResponse<SectionDetailDTO>> updateSection(
            @PathVariable UUID sectionId,
            @Valid @RequestBody SectionRequestDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(sectionService.updateSection(sectionId, dto)));
    }

    @DeleteMapping("/api/v1/admin/sections/{sectionId}")
    @Operation(summary = "Delete a section/zone", description = "Deletes a section/zone by its ID")
    public ResponseEntity<ApiResponse<Void>> deleteSection(@PathVariable UUID sectionId) {
        sectionService.deleteSection(sectionId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
