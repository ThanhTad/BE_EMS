package io.event.ems.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ticket_qr_codes")
@Data
public class TicketQrCode {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_seat_id")
    private EventSeatStatus eventSeat;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchased_ga_ticket_id")
    private PurchasedGaTicket purchasedGaTicket;

    @Column(nullable = false, unique = true, length = 100)
    private String uniqueIdentifier;

    @Column(nullable = false, columnDefinition = "text")
    private String qrCodeData;

    @CreationTimestamp // Tự động điền thời gian tạo
    @Column(nullable = false, updatable = false)
    private Instant generatedAt;

    @Column
    private Instant checkInAt;
}
