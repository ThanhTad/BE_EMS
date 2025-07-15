package io.event.ems.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "seat_maps")
@Getter
@Setter
@NoArgsConstructor
public class SeatMap {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @OneToMany(mappedBy = "seatMap", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 20)
    @JsonManagedReference("seatmap-section")
    private Set<SeatSection> sections = new HashSet<>();

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private JsonNode layoutData;

    private String description;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // Chỉ cần class là con của SeatSection là được, không cần chính xác là SeatSection
        if (o == null || !(o instanceof SeatSection)) return false;
        SeatSection that = (SeatSection) o;
        // Chỉ so sánh dựa trên ID.
        // ID không được null và phải bằng nhau.
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        // Chỉ hash dựa trên ID. Nếu ID là null, dùng hash mặc định.
        // Cách viết này phổ biến và an toàn.
        return id != null ? id.hashCode() : super.hashCode();
    }
}
