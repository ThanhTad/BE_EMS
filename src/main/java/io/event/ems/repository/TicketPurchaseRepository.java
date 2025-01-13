package io.event.ems.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.event.ems.model.Ticket;
import io.event.ems.model.TicketPurchase;
import io.event.ems.model.User;

@Repository
public interface TicketPurchaseRepository extends JpaRepository<TicketPurchase, UUID> {

    Page<TicketPurchase> findByUserId(UUID userId, Pageable pageable);
    Page<TicketPurchase> findByTicketId(UUID ticketId, Pageable pageable);
    Page<TicketPurchase> findByStatusId(Integer statusId, Pageable pageable);
    boolean existByUserAndTicket(User user, Ticket ticket);

}
