package io.event.ems.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import io.event.ems.dto.TicketDTO;
import io.event.ems.exception.ResourceNotFoundException;

public interface TicketService {

    Page<TicketDTO> getAllTicket(Pageable pageable);
    Optional<TicketDTO> getTicketById(UUID id) throws ResourceNotFoundException;
    TicketDTO createTicket(TicketDTO ticketDTO);
    TicketDTO updateTicket(UUID id, TicketDTO ticketDTO) throws ResourceNotFoundException;
    void deleteTicket(UUID id) throws ResourceNotFoundException;
    Page<TicketDTO> getTicketByEventId(UUID eventId, Pageable pageable);
    Page<TicketDTO> getTicketByStatusId(Integer statusId, Pageable pageable);
    

}
