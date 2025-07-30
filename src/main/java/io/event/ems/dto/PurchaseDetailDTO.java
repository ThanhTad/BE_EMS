package io.event.ems.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class PurchaseDetailDTO {

    private UUID id;
    private LocalDateTime purchaseDate;
    private BigDecimal subTotal;
    private BigDecimal serviceFee;
    private BigDecimal totalPrice;
    private String currency;
    private String status;
    private String paymentMethod;
    private String transactionId;

    private CustomerInfoDTO customer;
    private EventInfoDTO event;

    private List<PurchasedGATicketDTO> generalAdmissionTickets;
    private List<PurchasedSeatedTicketDTO> seatedTickets;

    @Data
    public static class CustomerInfoDTO {
        private UUID id;
        private String fullName;
        private String email;
        private String phoneNumber;
    }

    @Data
    public static class EventInfoDTO {
        private UUID id;
        private String title;
        private String slug;
        private LocalDateTime startDate;
    }

    @Data
    public static class PurchasedGATicketDTO {
        private String ticketName;
        private int quantity;
        private BigDecimal pricePerTicket;
    }

    @Data
    public static class PurchasedSeatedTicketDTO {
        private String sectionName;
        private String rowLabel;
        private String seatNumber;
        private String ticketName;
        private BigDecimal priceAtPurchase;
    }
}
