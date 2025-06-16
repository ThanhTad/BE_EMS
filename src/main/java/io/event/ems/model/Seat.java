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

@Entity
@Table(name = "seats")
@Data
@NoArgsConstructor
public class Seat {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    private String id;

    @Column(nullable = false)
    private String rowLabel;

    @Column(nullable = false)
    private String seatNumber;

    private String seatType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private SeatSection section;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private JsonNode coordinates;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
