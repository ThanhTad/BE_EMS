package io.event.ems.repository;

import io.event.ems.model.SeatMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SeatMapRepository extends JpaRepository<SeatMap, UUID> {

    Page<SeatMap> findByVenueId(UUID veinId, Pageable pageable);
}
