package io.event.ems.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;

@Data
public class TicketPurchaseDetailDTO {

    private UUID id;
    private Integer quantity;
    private BigDecimal totalPrice;
    private LocalDateTime purchaseDate;
    private Integer statusId;

    private String ticketType;
    private EventInfoDTO event;

}
