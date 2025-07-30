package io.event.ems.repository;

import io.event.ems.model.PurchasedGATicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PurchasedGATicketRepository extends JpaRepository<PurchasedGATicket, UUID> {

    List<PurchasedGATicket> findByTicketPurchaseId(UUID ticketPurchaseId);
}
