package io.event.ems.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class ZonedAdmissionDTO {

    private UUID seatMapId;
    private String seatMapName;
    private List<ZoneDTO> zones;
    private JsonNode layoutData;
}
