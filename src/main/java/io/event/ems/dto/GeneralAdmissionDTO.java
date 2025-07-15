package io.event.ems.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class GeneralAdmissionDTO {

    private List<TicketTypeDTO> availableTickets;
    private int totalCapacity;
    private int availableCapacity;
}
