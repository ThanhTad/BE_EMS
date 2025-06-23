package io.event.ems.repository;

import io.event.ems.model.PurchasedGaTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PurchasedGaTicketRepository extends JpaRepository<PurchasedGaTicket, UUID> {

    List<PurchasedGaTicket> findByTicketPurchaseId(UUID ticketPurchaseId);
}
