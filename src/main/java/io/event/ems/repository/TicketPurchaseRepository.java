package io.event.ems.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import io.event.ems.model.Ticket;
import io.event.ems.model.TicketPurchase;
import io.event.ems.model.User;

@Repository
public interface TicketPurchaseRepository extends JpaRepository<TicketPurchase, UUID> {

    Page<TicketPurchase> findByUserId(UUID userId, Pageable pageable);

    Page<TicketPurchase> findByTicketId(UUID ticketId, Pageable pageable);

    Page<TicketPurchase> findByStatusId(Integer statusId, Pageable pageable);

    boolean existsByUserAndTicket(User user, Ticket ticket);

    @Query("SELECT COALESCE(SUM(tp.quantity), 0) FROM TicketPurchase tp JOIN tp.ticket t WHERE tp.user.id = :userId AND t.event.id = :eventId AND tp.status.status = 'SUCCESS'")
    int countTotalTicketsByUserAndEvent(UUID userId, UUID eventId);

    List<TicketPurchase> findByTransactionId(String transactionId);
}
