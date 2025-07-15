package io.event.ems.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class TicketTypeDTO {

    private UUID ticketId;
    private String name;
    private BigDecimal price;
    private String description;
    private Integer totalQuantity;
    private Integer availableQuantity;
    private Integer maxPerPurchase;
    private LocalDateTime saleStartDate;
    private LocalDateTime saleEndDate;
    private boolean isOnSale;
}
