package io.event.ems.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ticket_qr_codes", indexes = {
        @Index(name = "idx_ticketqrcode_eventseat", columnList = "event_seat_id", unique = true),
        @Index(name = "idx_ticketqrcode_uniqueidentifier", columnList = "uniqueIdentifier", unique = true)
})
@Data
public class TicketQrCode {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_seat_id")
    private EventSeatStatus eventSeat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchased_ga_ticket_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private PurchasedGATicket purchasedGaTicket;

    @Column(nullable = false, unique = true, length = 100)
    private String uniqueIdentifier;

    @Column(nullable = false, columnDefinition = "text")
    private String qrCodeData;

    @CreationTimestamp // Tự động điền thời gian tạo
    @Column(nullable = false, updatable = false)
    private Instant generatedAt;

    @Column
    private Instant checkInAt;

    // Thêm trường version cho optimistic locking
    @Version
    private Long version;
}
