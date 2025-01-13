package io.event.ems.service.impl;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.event.ems.dto.TicketPurchaseDTO;
import io.event.ems.exception.DuplicateResourceException;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.mapper.TicketPurchaseMapper;
import io.event.ems.model.StatusCode;
import io.event.ems.model.Ticket;
import io.event.ems.model.TicketPurchase;
import io.event.ems.model.User;
import io.event.ems.repository.StatusCodeRepository;
import io.event.ems.repository.TicketPurchaseRepository;
import io.event.ems.repository.TicketRepository;
import io.event.ems.repository.UserRepository;
import io.event.ems.service.TicketPurchaseService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TicketPurchaseServiceImpl implements TicketPurchaseService {

    private final TicketPurchaseRepository ticketPurchaseRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final StatusCodeRepository statusCodeRepository;
    private final TicketPurchaseMapper ticketPurchaseMapper;

    @Override
    public Page<TicketPurchaseDTO> getAllTicketPurchases(Pageable pageable) {
        return ticketPurchaseRepository.findAll(pageable)
                    .map(ticketPurchaseMapper::toDTO);
    }

    @Override
    public Optional<TicketPurchaseDTO> getTicketPurchaseById(UUID id) throws ResourceNotFoundException {
        return ticketPurchaseRepository.findById(id)
                    .map(ticketPurchaseMapper::toDTO);
    }

    @Override
    public TicketPurchaseDTO createTicketPurchase(TicketPurchaseDTO ticketPurchaseDTO)
            throws ResourceNotFoundException {
        User user = userRepository.findById(ticketPurchaseDTO.getUserId())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + ticketPurchaseDTO.getUserId()));
        Ticket ticket = ticketRepository.findById(ticketPurchaseDTO.getTicketId())
                        .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketPurchaseDTO.getTicketId()));
        StatusCode status = statusCodeRepository.findById(ticketPurchaseDTO.getStatusId())
                                .orElseThrow(() -> new ResourceNotFoundException("Status not found with id: " + ticketPurchaseDTO.getStatusId()));
        
        // Check if the user has already purchased the same ticket
        if(ticketPurchaseRepository.existByUserAndTicket(user, ticket)){
            throw new DuplicateResourceException("User has already purchased this ticket");
        }

        // Check available ticket quantity
        if(ticket.getAvailableQuantity() < ticketPurchaseDTO.getQuantity()){
            throw new IllegalArgumentException("Not enough tickets available");
        }

        TicketPurchase ticketPurchase = ticketPurchaseMapper.toEntity(ticketPurchaseDTO);
        ticketPurchase.setUser(user);
        ticketPurchase.setTicket(ticket);
        ticketPurchase.setStatus(status);

        // Calculate total price
        BigDecimal totalPrice = ticket.getPrice().multiply(BigDecimal.valueOf(ticketPurchaseDTO.getQuantity()));
        ticketPurchase.setTotalPrice(totalPrice);

        // Reduce available ticket quantity
        ticket.setAvailableQuantity(ticket.getAvailableQuantity() - ticketPurchaseDTO.getQuantity());
        ticketRepository.save(ticket);

        TicketPurchase saveTicketPurchase = ticketPurchaseRepository.save(ticketPurchase);
        return ticketPurchaseMapper.toDTO(saveTicketPurchase);

    }
   

    @Override
    public TicketPurchaseDTO updateTicketPurchase(UUID id, TicketPurchaseDTO ticketPurchaseDTO)
            throws ResourceNotFoundException {
        
        TicketPurchase ticketPurchase = ticketPurchaseRepository.findById(id)
                                            .orElseThrow(() -> new ResourceNotFoundException("Ticket purchase not found with id: " + id));
        
        if(ticketPurchaseDTO.getUserId() != null){
            User user = userRepository.findById(ticketPurchaseDTO.getUserId())
                            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + ticketPurchaseDTO.getUserId()));
            ticketPurchase.setUser(user);
        }

        if(ticketPurchaseDTO.getStatusId() != null){
            StatusCode statusCode = statusCodeRepository.findById(ticketPurchaseDTO.getStatusId())
                                        .orElseThrow(() -> new ResourceNotFoundException("Status not found with id: " + ticketPurchaseDTO.getStatusId()));
            ticketPurchase.setStatus(statusCode);
        }

        if(ticketPurchaseDTO.getTicketId() != null){
            Ticket ticket = ticketRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketPurchaseDTO.getTicketId()));
            
            // Check if the user has already purchased the same ticket
            if(ticketPurchaseRepository.existByUserAndTicket(ticketPurchase.getUser(), ticket)){
                throw new IllegalArgumentException("User has already purchased this ticket");
            }
            ticketPurchase.setTicket(ticket);
        }

         // Prevent updating certain fields
         ticketPurchase.setPurchaseDate(ticketPurchase.getPurchaseDate()); // Keep the original purchase date
         ticketPurchase.setTotalPrice(ticketPurchase.getTotalPrice()); // Keep the original total price
 
         ticketPurchaseMapper.updateTicketPurchaseFromDTO(ticketPurchaseDTO, ticketPurchase);
 
         TicketPurchase updatedTicketPurchase = ticketPurchaseRepository.save(ticketPurchase);
         return ticketPurchaseMapper.toDTO(updatedTicketPurchase);
        
    }

    @Override
    public void deleteTicketPurchase(UUID id) throws ResourceNotFoundException {
        TicketPurchase ticketPurchase = ticketPurchaseRepository.findById(id)
                                            .orElseThrow(() -> new ResourceNotFoundException("Ticket purchase not found with id: " + id));

        Ticket ticket = ticketPurchase.getTicket();
        ticket.setAvailableQuantity(ticket.getAvailableQuantity() + ticketPurchase.getQuantity());
        ticketRepository.save(ticket);

        ticketPurchaseRepository.delete(ticketPurchase);
    }

    @Override
    public Page<TicketPurchaseDTO> getTicketPurchasesByUserId(UUID userId, Pageable pageable) {
        return ticketPurchaseRepository.findByUserId(userId, pageable)
                    .map(ticketPurchaseMapper::toDTO);
    }

    @Override
    public Page<TicketPurchaseDTO> getTicketPurchasesByTicketId(UUID ticketId, Pageable pageable) {
        return ticketPurchaseRepository.findByTicketId(ticketId, pageable)
                    .map(ticketPurchaseMapper::toDTO);
    }

    @Override
    public Page<TicketPurchaseDTO> getTicketPurchasesByStatusId(Integer statusId, Pageable pageable) {
        return ticketPurchaseRepository.findByStatusId(statusId, pageable)
                    .map(ticketPurchaseMapper::toDTO);
    }

}
