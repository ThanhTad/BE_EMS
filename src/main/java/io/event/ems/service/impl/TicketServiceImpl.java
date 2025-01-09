package io.event.ems.service.impl;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.event.ems.dto.TicketDTO;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.mapper.TicketMapper;
import io.event.ems.model.Event;
import io.event.ems.model.StatusCode;
import io.event.ems.model.Ticket;
import io.event.ems.repository.EventRepository;
import io.event.ems.repository.StatusCodeRepository;
import io.event.ems.repository.TicketRepository;
import io.event.ems.service.TicketService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TicketServiceImpl implements TicketService {
    
    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final StatusCodeRepository statusCodeRepository;
    private final TicketMapper ticketMapper;
    
    @Override
    public Page<TicketDTO> getAllTicket(Pageable pageable) {
       return ticketRepository.findAll(pageable)
                    .map(ticketMapper::toDTO);
    }

    @Override
    public Optional<TicketDTO> getTicketById(UUID id) throws ResourceNotFoundException {
        return ticketRepository.findById(id)
                    .map(ticketMapper::toDTO);
    }

    @Override
    public TicketDTO createTicket(TicketDTO ticketDTO) {
        
        Event event = eventRepository.findById(ticketDTO.getEventId())
                        .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + ticketDTO.getEventId()));
        StatusCode statusCode = statusCodeRepository.findById(ticketDTO.getStatusId())
                                    .orElseThrow(() -> new ResourceNotFoundException("Status not found with id: " + ticketDTO.getStatusId()));
        Ticket newTicket = ticketMapper.toEntity(ticketDTO);
        newTicket.setEvent(event);
        newTicket.setStatus(statusCode);
        Ticket saveTicket = ticketRepository.save(newTicket);
        return ticketMapper.toDTO(saveTicket);

    }

    @Override
    public TicketDTO updateTicket(UUID id, TicketDTO ticketDTO) throws ResourceNotFoundException {

        Ticket ticket = ticketRepository.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));
       
        if(ticketDTO.getEventId() != null){
            Event event = eventRepository.findById(ticketDTO.getEventId())
                            .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " +ticketDTO.getEventId()));
            ticket.setEvent(event);
        }

        if(ticketDTO.getStatusId() != null){
            StatusCode statusCode = statusCodeRepository.findById(ticketDTO.getStatusId())
                                        .orElseThrow(() -> new ResourceNotFoundException("Status not found with id: " + ticketDTO.getEventId()));
            ticket.setStatus(statusCode);
        }

        ticketMapper.updateTicketFromDTO(ticketDTO, ticket);
        Ticket updatedTicket = ticketRepository.save(ticket);

        return ticketMapper.toDTO(updatedTicket);

    }

    @Override
    public void deleteTicket(UUID id) throws ResourceNotFoundException {
        if(!ticketRepository.existsById(id)){
            throw new ResourceNotFoundException("Ticket not found with id: " + id);
        }
        ticketRepository.deleteById(id);
    }

    @Override
    public Page<TicketDTO> getTicketByEventId(UUID eventId, Pageable pageable) {
        return ticketRepository.findByEventId(eventId, pageable)
                    .map(ticketMapper::toDTO);
    }

    @Override
    public Page<TicketDTO> getTicketByStatusId(Integer statusId, Pageable pageable) {
       return ticketRepository.findByStatusId(statusId, pageable)
                    .map(ticketMapper::toDTO);
    }

}
