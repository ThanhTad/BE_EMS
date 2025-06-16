package io.event.ems.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Data
@Table(name = "events", indexes = {
        @Index(name = "idx_event_title", columnList = "title"),
        @Index(name = "idx_event_slug", columnList = "slug"),
        @Index(name = "idx_event_start_date", columnList = "start_date"),
        @Index(name = "idx_event_creator_id", columnList = "creator_id"),
        @Index(name = "idx_event_status_id", columnList = "status_id"),
        @Index(name = "idx_event_venue_id", columnList = "venue_id"),
        @Index(name = "idx_event_public", columnList = "is_public"),
        @Index(name = "idx_event_dates", columnList = "start_date, end_date")})
public class Event {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(unique = true, nullable = false)
    private String slug;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @ManyToMany
    @JoinTable(name = "event_categories", joinColumns = @JoinColumn(name = "event_id"), inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> categories = new HashSet<>();

    @ManyToOne(optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_selection_mode", nullable = false)
    private TicketSelectionModeEnum ticketSelectionMode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_map_id")
    private SeatMap seatMap;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id")
    private StatusCode status;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "is_public", columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean isPublic;

    @Column(name = "cover_image_url")
    @Size(max = 255)
    private String coverImageUrl;

    private Double latitude;
    private Double longitude;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

}
