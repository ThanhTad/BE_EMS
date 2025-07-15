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

    @Query("SELECT t FROM Ticket t WHERE t.event.id = :eventId AND t.appliesToSection.id = :sectionId")
    List<Ticket> findByEventIdAndSectionId(@Param("eventId") UUID eventId, @Param("sectionId") UUID sectionId);

    @Query("SELECT t FROM Ticket t WHERE t.event.id = :eventId AND t.appliesToSection IS NULL")
    List<Ticket> findGeneralAdmissionTicketsByEventId(@Param("eventId") UUID eventId);

    @Query("SELECT t FROM Ticket t WHERE t.event.id = :eventId AND t.appliesToSection.id IN :sectionIds")
    List<Ticket> findByEventIdAndSectionIdIn(@Param("eventId") UUID eventId, @Param("sectionIds") List<UUID> sectionIds);

    @Modifying
    @Query("UPDATE Ticket t SET t.availableQuantity = t.availableQuantity - :quantity " +
            "WHERE t.id = :ticketId AND t.availableQuantity >= :quantity")
    int decreaseAvailableQuantity(@Param("ticketId") UUID ticketId, @Param("quantity") int quantity);

    @Modifying
    @Query("UPDATE Ticket t SET t.availableQuantity = t.availableQuantity + :quantity WHERE t.id = :ticketId")
    void increaseAvailableQuantity(@Param("ticketId") UUID ticketId, @Param("quantity") int quantity);

    List<Ticket> findByEventIdAndNameContainingIgnoreCase(UUID eventId, String name);
}
