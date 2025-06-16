package io.event.ems.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class SeatLayoutDTO {

    private String sectionName;
    private String rowLabel;
    private String seatNumber;
    private String seatType;
    private JsonNode coordinates;
}
