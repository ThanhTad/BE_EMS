package io.event.ems.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
@Table(name = "seat_sections")
@Setter
@Getter
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
    @JsonBackReference("seatmap-section")
    private SeatMap seatMap;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 25)
    @JsonManagedReference("section-seat")
    private Set<Seat> seats = new HashSet<>();

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private JsonNode layoutData;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Thêm một Seat vào SeatSection này và đồng bộ hóa mối quan hệ.
     *
     * @param seat Seat cần thêm.
     */
    public void addSeat(Seat seat) {
        if (seat != null) {
            seats.add(seat);
            seat.setSection(this);
        }
    }

    /**
     * Xóa một Seat khỏi SeatSection này và đồng bộ hóa mối quan hệ.
     *
     * @param seat Seat cần xóa.
     */
    public void removeSeat(Seat seat) {
        if (seat != null) {
            seats.remove(seat);
            seat.setSection(null);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // Chỉ cần class là con của SeatSection là được, không cần chính xác là SeatSection
        if (!(o instanceof Seat that)) return false;
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
