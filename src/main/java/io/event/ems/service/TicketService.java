package io.event.ems.service;

import io.event.ems.dto.TicketRequestDTO;
import io.event.ems.dto.TicketResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface TicketService {

    TicketResponseDTO createTicketForEvent(UUID eventId, TicketRequestDTO requestDto);

    Page<TicketResponseDTO> getTicketsForEvent(UUID eventId, Pageable pageable);

    Optional<TicketResponseDTO> getTicketById(UUID eventId, UUID ticketId);

    TicketResponseDTO updateTicket(UUID eventId, UUID ticketId, TicketRequestDTO requestDto);

    void deleteTicket(UUID eventId, UUID ticketId);

}
