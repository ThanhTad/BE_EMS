package io.event.ems.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class ZoneDTO {

    private UUID zoneId;
    private String name;
    private int capacity;
    private int availableCapacity;
    private List<TicketTypeDTO> availableTickets;
    private String status;
    private JsonNode layoutData;
}
