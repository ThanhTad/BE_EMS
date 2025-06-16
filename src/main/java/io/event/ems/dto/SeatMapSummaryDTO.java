package io.event.ems.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class SeatMapSummaryDTO {

    private UUID id;
    private String name;
    private String venueName;
    private UUID venueId;
}
