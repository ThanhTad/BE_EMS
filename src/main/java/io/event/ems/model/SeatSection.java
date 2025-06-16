package io.event.ems.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "seat_sections")
@Data
@NoArgsConstructor
public class SeatSection {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private int capacity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_map_id", nullable = false)
    private SeatMap seatMap;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Seat> seats = new ArrayList<>();

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private JsonNode layoutData;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
