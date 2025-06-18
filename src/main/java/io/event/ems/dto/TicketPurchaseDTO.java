package io.event.ems.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class TicketPurchaseDTO {

    private UUID id;
    private UUID eventId;
    private String eventTitle;
    private UUID userId;
    private LocalDateTime purchaseDate;
    private BigDecimal subTotal;
    private BigDecimal serviceFee;
    private BigDecimal totalPrice;
    private String currency;
    private String status;
    private String paymentMethod;
    private String transactionId;
    private List<PurchasedTicketDetailDTO> purchasedTickets;

}
