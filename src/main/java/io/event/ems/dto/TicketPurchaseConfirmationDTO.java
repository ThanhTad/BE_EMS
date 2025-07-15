package io.event.ems.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class TicketPurchaseConfirmationDTO {

    private UUID purchaseId;
    private String message;
    private LocalDateTime purchaseDate;
}
