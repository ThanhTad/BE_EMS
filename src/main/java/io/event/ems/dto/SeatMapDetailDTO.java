package io.event.ems.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object for detailed view of SeatMap entity
 */
@Data
@NoArgsConstructor
public class SeatMapDetailDTO {

    private UUID id;
    private String name;
    private UUID venueId;
    private String venueName;
    private List<SectionDetailDTO> sections;
    private JsonNode layoutData;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}