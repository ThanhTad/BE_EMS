package io.event.ems.repository;

import io.event.ems.model.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    Page<Ticket> findByEventId(UUID eventId, Pageable pageable);

    Optional<Ticket> findByIdAndEventId(UUID id, UUID eventId);

    @Modifying
    @Query("UPDATE Ticket t SET t.availableQuantity = t.availableQuantity - :quantity " +
            "WHERE t.id = :ticketId AND t.availableQuantity >= :quantity")
    int decreaseAvailableQuantity(@Param("ticketId") UUID ticketId, @Param("quantity") int quantity);

    @Query("SELECT t FROM Ticket t " +
            "JOIN FETCH t.appliesToSection s " +
            "WHERE t.event.id = :eventId AND s.id IN :sectionIds")
    List<Ticket> findByEventIdAndSectionIdsWithDetails(
            @Param("eventId") UUID eventId,
            @Param("sectionIds") List<UUID> sectionIds
    );
}
