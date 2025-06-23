package io.event.ems.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Data
@Table(name = "purchased_ga_tickets")
public class PurchasedGaTicket {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ticket_purchase_id", nullable = false)
    private TicketPurchase ticketPurchase;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "price_per_ticket", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerTicket;

    @OneToOne(mappedBy = "purchasedGaTicket", cascade = CascadeType.ALL)
    private TicketQrCode ticketQrCode;
}