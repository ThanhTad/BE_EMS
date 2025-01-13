package io.event.ems.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import io.event.ems.dto.TicketPurchaseDTO;
import io.event.ems.exception.ResourceNotFoundException;

public interface TicketPurchaseService {

    Page<TicketPurchaseDTO> getAllTicketPurchases(Pageable pageable);
    Optional<TicketPurchaseDTO> getTicketPurchaseById(UUID id) throws ResourceNotFoundException;
    TicketPurchaseDTO createTicketPurchase(TicketPurchaseDTO ticketPurchaseDTO) throws ResourceNotFoundException;
    TicketPurchaseDTO updateTicketPurchase(UUID id, TicketPurchaseDTO ticketPurchaseDTO) throws ResourceNotFoundException;
    void deleteTicketPurchase(UUID id) throws ResourceNotFoundException;
    Page<TicketPurchaseDTO> getTicketPurchasesByUserId(UUID userId, Pageable pageable);
    Page<TicketPurchaseDTO> getTicketPurchasesByTicketId(UUID ticketId, Pageable pageable);
    Page<TicketPurchaseDTO> getTicketPurchasesByStatusId(Integer statusId, Pageable pageable);

}
