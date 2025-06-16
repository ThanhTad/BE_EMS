package io.event.ems.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class SeatMapLayoutDTO {

    private String mapName;
    private UUID venueId;
    private List<ZoneLayoutDTO> zones;
    private List<SeatLayoutDTO> seats;
}
