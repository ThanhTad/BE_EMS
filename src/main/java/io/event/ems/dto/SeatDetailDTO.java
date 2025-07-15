package io.event.ems.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for detailed view of Seat entity
 */
@Data
@NoArgsConstructor
public class SeatDetailDTO {

    private UUID id;
    private String rowLabel;
    private String seatNumber;
    private String seatType;
    private UUID sectionId;
    private String sectionName;
    private JsonNode coordinates;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}