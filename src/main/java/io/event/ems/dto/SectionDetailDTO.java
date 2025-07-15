package io.event.ems.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object for detailed view of SeatSection entity
 */
@Data
@NoArgsConstructor
public class SectionDetailDTO {

    private UUID id;
    private String name;
    private int capacity;
    private UUID seatMapId;
    private List<SeatDetailDTO> seats;
    private JsonNode layoutData;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}