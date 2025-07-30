package io.event.ems.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class PurchaseListItemDTO {

    private UUID id;
    private String eventTitle;
    private UUID eventId;
    private String eventImageUrl;
    private String customerName;
    private LocalDateTime purchaseDate;
    private BigDecimal totalPrice;
    private String status;
    private String currency;
}
