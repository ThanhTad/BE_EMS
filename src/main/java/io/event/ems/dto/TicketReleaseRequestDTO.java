package io.event.ems.dto;

import io.event.ems.model.TicketSelectionModeEnum;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class TicketReleaseRequestDTO {

    private TicketSelectionModeEnum selectionMode;
    private UUID ticketId;
    private List<UUID> seatIds;
    private UUID zoneId;
    private int quantity;
}
