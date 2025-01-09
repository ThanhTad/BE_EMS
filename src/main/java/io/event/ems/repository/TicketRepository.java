package io.event.ems.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.event.ems.model.Ticket;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    Page<Ticket> findByEventId(UUID eventId, Pageable pageable);
    Page<Ticket> findByStatusId(Integer statusId, Pageable pageable);

}
