package io.event.ems.service.impl;

import io.event.ems.dto.TicketRequestDTO;
import io.event.ems.dto.TicketResponseDTO;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.mapper.TicketMapper;
import io.event.ems.model.Event;
import io.event.ems.model.SeatSection;
import io.event.ems.model.StatusCode;
import io.event.ems.model.Ticket;
import io.event.ems.repository.EventRepository;
import io.event.ems.repository.SeatSectionRepository;
import io.event.ems.repository.StatusCodeRepository;
import io.event.ems.repository.TicketRepository;
import io.event.ems.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final StatusCodeRepository statusCodeRepository;
    private final SeatSectionRepository seatSectionRepository;
    private final TicketMapper ticketMapper;

    @Override
    public TicketResponseDTO createTicketForEvent(UUID eventId, TicketRequestDTO ticketRequestDTO) {
        log.info("Creating ticket for event: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        Ticket ticket = ticketMapper.toEntity(ticketRequestDTO);
        ticket.setEvent(event);

        // Validate và set section nếu có
        if (ticketRequestDTO.getAppliesToSectionId() != null) {
            SeatSection seatSection = seatSectionRepository.findById(ticketRequestDTO.getAppliesToSectionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Section not found with id: " + ticketRequestDTO.getAppliesToSectionId()));
            ticket.setAppliesToSection(seatSection);
        }

        StatusCode statusCode = statusCodeRepository.findById(ticketRequestDTO.getStatusId())
                .orElseThrow(() -> new ResourceNotFoundException("Status not found with id: " + ticketRequestDTO.getStatusId()));
        ticket.setStatus(statusCode);

        if (ticketRequestDTO.getTotalQuantity() != null) {
            ticket.setAvailableQuantity(ticketRequestDTO.getTotalQuantity());
        }

        Ticket saveTicket = ticketRepository.save(ticket);
        return ticketMapper.toDTO(saveTicket);

    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketResponseDTO> getTicketsForEvent(UUID eventId, Pageable pageable) {
        log.info("Getting tickets for event: {}", eventId);
        return ticketRepository.findByEventId(eventId, pageable)
                .map(ticketMapper::toDTO);
    }


    @Override
    @Transactional(readOnly = true)
    public Optional<TicketResponseDTO> getTicketById(UUID eventId, UUID ticketId) throws ResourceNotFoundException {
        log.info("Getting ticket for event: {} and ticketId: {}", eventId, ticketId);
        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event not found with id: " + eventId);
        }
        return ticketRepository.findByIdAndEventId(ticketId, eventId)
                .map(ticketMapper::toDTO);
    }

    @Override
    public TicketResponseDTO updateTicket(UUID eventId, UUID ticketId, TicketRequestDTO ticketRequestDTO) throws ResourceNotFoundException {
        log.info("Updating ticket for event: {} and ticketId: {}", eventId, ticketId);

        Ticket existingTicket = ticketRepository.findByIdAndEventId(ticketId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId + "event id: " + eventId));

        ticketMapper.updateTicketFromDTO(ticketRequestDTO, existingTicket);

        if (ticketRequestDTO.getAppliesToSectionId() != null) {
            SeatSection seatSection = seatSectionRepository.findById(ticketRequestDTO.getAppliesToSectionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Section not found with id: " + ticketRequestDTO.getAppliesToSectionId()));
            existingTicket.setAppliesToSection(seatSection);
        }

        if (ticketRequestDTO.getStatusId() != null) {
            StatusCode statusCode = statusCodeRepository.findById(ticketRequestDTO.getStatusId())
                    .orElseThrow(() -> new ResourceNotFoundException("Status not found with id: " + ticketRequestDTO.getStatusId()));
            existingTicket.setStatus(statusCode);
        }

        Ticket updatedTicket = ticketRepository.save(existingTicket);
        return ticketMapper.toDTO(updatedTicket);

    }

    @Override
    public void deleteTicket(UUID eventId, UUID ticketId) throws ResourceNotFoundException {
        log.info("Deleting ticket for event: {} and ticketId: {}", eventId, ticketId);
        Ticket ticket = ticketRepository.findByIdAndEventId(ticketId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId + "event id: " + eventId));

        if (ticket.getTotalQuantity() != null && ticket.getAvailableQuantity() != null) {
            int soldQuantity = ticket.getTotalQuantity() - ticket.getAvailableQuantity();
            if (soldQuantity > 0) {
                throw new IllegalStateException("Cannot delete ticket that has already been sold");
            }
        }
        ticketRepository.deleteById(ticketId);
    }
}
