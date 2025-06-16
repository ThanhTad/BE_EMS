package io.event.ems.repository;

import io.event.ems.model.SeatSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SeatSectionRepository extends JpaRepository<SeatSection, UUID> {
}
