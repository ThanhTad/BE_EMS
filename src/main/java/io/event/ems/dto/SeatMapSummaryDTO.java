package io.event.ems.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for a summary view of SeatMap entity
 */
@Data
@NoArgsConstructor
public class SeatMapSummaryDTO {

    private UUID id;
    private String name;
    private UUID venueId;
    private String venueName;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}