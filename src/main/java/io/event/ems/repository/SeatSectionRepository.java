package io.event.ems.repository;

import io.event.ems.model.SeatSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SeatSectionRepository extends JpaRepository<SeatSection, UUID> {

    List<SeatSection> findBySeatMapId(UUID seatMapId);
}
