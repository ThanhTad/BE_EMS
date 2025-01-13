package io.event.ems.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TicketPurchaseDTO {

    private UUID id;

    @NotNull(message = "User Id cannot be null")
    private UUID userId;

    
    @NotNull(message = "Ticket ID cannot be null")
    private UUID ticketId;

    @NotNull(message = "Quantity cannot be null")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    private LocalDateTime purchaseDate;

    private BigDecimal totalPrice;

    @NotNull(message = "Status ID cannot be null")
    private Integer statusId;

    private String paymentMethod;

    private String transactionId;

}
