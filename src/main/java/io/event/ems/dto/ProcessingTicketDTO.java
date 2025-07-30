package io.event.ems.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProcessingTicketDTO {
    private String ticketName;
    private EmailDetails.TicketInfo ticketInfo;
}
