package io.event.ems.repository;

import io.event.ems.dto.SectionAvailabilityDTO;
import io.event.ems.model.EventSeatStatus;
import io.event.ems.model.SeatSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventSeatStatusRepository extends JpaRepository<EventSeatStatus, UUID> {

    @Query("SELECT ess FROM EventSeatStatus ess " +
            "JOIN FETCH ess.seat s " +
            "JOIN FETCH s.section sec " +
            "LEFT JOIN FETCH ess.ticket t " +
            "WHERE ess.event.id = :eventId AND sec.seatMap.id = :seatMapId")
    List<EventSeatStatus> findByEventIdAndSeatMapId(@Param("eventId") UUID eventId,
                                                    @Param("seatMapId") UUID seatMapId);

    @Query("SELECT COUNT(ess) FROM EventSeatStatus ess " +
            "WHERE ess.event.id = :eventId AND ess.seat.section.id = :sectionId " +
            "AND ess.status = 'available'")
    int countAvailableSeatsInSection(@Param("eventId") UUID eventId,
                                     @Param("sectionId") UUID sectionId);

    @Query("SELECT ess FROM EventSeatStatus ess " +
            "WHERE ess.event.id = :eventId AND ess.seat.id IN :seatIds")
    List<EventSeatStatus> findAllByEventIdAndSeatIdIn(@Param("eventId") UUID eventId,
                                                      @Param("seatIds") List<UUID> seatIds);

    @Query("SELECT ess FROM EventSeatStatus ess " +
            "JOIN FETCH ess.ticket t " +
            "JOIN FETCH ess.seat s " +
            "JOIN FETCH s.section sec " +
            "WHERE ess.ticketPurchase.id = :purchaseId")
    List<EventSeatStatus> findByTicketPurchaseId(@Param("purchaseId") UUID purchaseId);

    @Query("SELECT DISTINCT s.section FROM Seat s WHERE s.id IN :seatIds")
    List<SeatSection> findSectionsForSeats(@Param("seatIds") List<UUID> seatIds);

    @Query("SELECT ess FROM EventSeatStatus ess JOIN FETCH ess.ticket t " +
            "WHERE ess.event.id = :eventId AND ess.seat.id IN :seatIds")
    List<EventSeatStatus> findAllByEventIdAndSeatIdInWithTicket(
            @Param("eventId") UUID eventId,
            @Param("seatIds") List<UUID> seatIds
    );

    long countAvailableSeatsByEventId(UUID eventId);

    @Query("SELECT new io.event.ems.dto.SectionAvailabilityDTO(s.seat.section.id, COUNT(s.id)) " +
            "FROM EventSeatStatus s " +
            "WHERE s.event.id = :eventId " +
            "AND s.seat.section.id IN :sectionIds " +
            "AND s.status = 'available' " +
            "GROUP BY s.seat.section.id")
    List<SectionAvailabilityDTO> countAvailableSeatsInSections(
            @Param("eventId") UUID eventId,
            @Param("sectionIds") List<UUID> sectionIds
    );

}
