package io.event.ems.repository;

import io.event.ems.model.SeatMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SeatMapRepository extends JpaRepository<SeatMap, UUID> {

    @Query("SELECT sm FROM SeatMap sm LEFT JOIN FETCH sm.sections WHERE sm.id = :seatMapId")
    Optional<SeatMap> findByIdWithSections(@Param("seatMapId") UUID seatMapId);

    @Query("SELECT sm FROM SeatMap sm " +
            "LEFT JOIN FETCH sm.sections s " +
            "LEFT JOIN FETCH s.seats " +
            "WHERE sm.id = :seatMapId")
    Optional<SeatMap> findByIdWithSectionsAndSeats(@Param("seatMapId") UUID seatMapId);
}
