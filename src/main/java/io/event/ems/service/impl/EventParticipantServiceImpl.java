package io.event.ems.service.impl;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.event.ems.dto.EventParticipantDTO;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.mapper.EventParticipantMapper;
import io.event.ems.model.Event;
import io.event.ems.model.EventParticipant;
import io.event.ems.model.StatusCode;
import io.event.ems.model.User;
import io.event.ems.repository.EventParticipantRepository;
import io.event.ems.repository.EventRepository;
import io.event.ems.repository.StatusCodeRepository;
import io.event.ems.repository.UserRepository;
import io.event.ems.service.EventParticipantService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class EventParticipantServiceImpl implements EventParticipantService {

    private final EventParticipantRepository repository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final StatusCodeRepository statusCodeRepository;
    private final EventParticipantMapper mapper;

    @Override
    public Page<EventParticipantDTO> getAllEventParticipant(Pageable pageable) {
        return repository.findAll(pageable)
                .map(mapper::toDTO);
    }

    @Override
    public Optional<EventParticipantDTO> getEventParticipantById(UUID id) throws ResourceNotFoundException {
        return repository.findById(id)
                .map(mapper::toDTO);
    }

    @Override
    public EventParticipantDTO createEventParticipant(EventParticipantDTO eventParticipantDTO)
            throws ResourceNotFoundException {
        User user = userRepository.findById(eventParticipantDTO.getUserId())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + eventParticipantDTO.getUserId()));
        Event event = eventRepository.findById(eventParticipantDTO.getEventId())
                        .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventParticipantDTO.getEventId()));
        StatusCode status = statusCodeRepository.findById(eventParticipantDTO.getStatusId())
                                .orElseThrow(() -> new ResourceNotFoundException("Status not found with id: " + eventParticipantDTO.getStatusId()));
    
        if(repository.existsByEventAndUser(event, user)){
            throw new IllegalArgumentException("User has already registered for this event");
        }

        EventParticipant eventParticipant = mapper.toEntity(eventParticipantDTO);
        eventParticipant.setUser(user);
        eventParticipant.setEvent(event);
        eventParticipant.setStatus(status);

        event.setCurrentParticipants(event.getCurrentParticipants() + 1 + eventParticipantDTO.getAdditionalGuests());
        eventRepository.save(event);

        EventParticipant saveEventParticipant = repository.save(eventParticipant);
        return mapper.toDTO(saveEventParticipant);

    }

    @Override
    public EventParticipantDTO updateEventParticipant(UUID id, EventParticipantDTO eventParticipantDTO)
            throws ResourceNotFoundException {
        
        EventParticipant eventParticipant = repository.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Event participant not found with id: " + id));
        Event event = eventParticipant.getEvent();
        if(eventParticipantDTO.getEventId() != null && !event.getId().equals(eventParticipantDTO.getEventId())){
            Event newEvent = eventRepository.findById(eventParticipantDTO.getEventId())
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventParticipantDTO.getEventId()));
            
            if(repository.existsByEventAndUser(newEvent, eventParticipant.getUser())){
                throw new IllegalArgumentException("User has already registered for this event");
            }

            event.setCurrentParticipants(Math.max(0, event.getCurrentParticipants() - 1 - eventParticipant.getAdditionalGuests()));
            eventRepository.save(event);

            eventParticipant.setEvent(event);

            event = newEvent;
            event.setCurrentParticipants(event.getCurrentParticipants() + 1 + eventParticipantDTO.getAdditionalGuests());
            eventRepository.save(event);

        }

        if(eventParticipantDTO.getUserId() != null && !eventParticipant.getUser().getId().equals(eventParticipantDTO.getUserId())){
          
            User newUser = userRepository.findById(eventParticipantDTO.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + eventParticipantDTO.getUserId()));

            
            if(repository.existsByEventAndUser(event, newUser)){
                throw new IllegalStateException("User has already registered for this event");
            }
            eventParticipant.setUser(newUser);
        }

        if(eventParticipantDTO.getStatusId() != null){
            StatusCode status = statusCodeRepository.findById(eventParticipantDTO.getStatusId())
                    .orElseThrow(() -> new ResourceNotFoundException("Status not found with id: " + eventParticipantDTO.getStatusId()));
        
            eventParticipant.setStatus(status);
        }

        if(eventParticipantDTO.getAdditionalGuests() != null && !eventParticipantDTO.getAdditionalGuests().equals(eventParticipant.getAdditionalGuests())){
            int oldAddGuest = eventParticipant.getAdditionalGuests();
            int newAddGuest = eventParticipantDTO.getAdditionalGuests();
            event.setCurrentParticipants(event.getCurrentParticipants() + newAddGuest - oldAddGuest);
            eventParticipant.setAdditionalGuests(newAddGuest);
            eventRepository.save(event);
        }

        mapper.updateEntityFromDTO(eventParticipantDTO, eventParticipant);
        EventParticipant updatedEventParticipant = repository.save(eventParticipant);
        return mapper.toDTO(updatedEventParticipant);

    }

    @Override
    public void deleteEventParticipant(UUID id) throws ResourceNotFoundException {
        EventParticipant eventParticipant = repository.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Event participant not found with id: " + id));
       
        Event event = eventParticipant.getEvent();
        event.setCurrentParticipants(Math.max(0, event.getCurrentParticipants() - 1 - eventParticipant.getAdditionalGuests()));
        eventRepository.save(event);

        repository.delete(eventParticipant);
    }

    @Override
    public Page<EventParticipantDTO> getEventParticipantsByEventId(UUID eventId, Pageable pageable) {
        return repository.findByEventId(eventId, pageable)
                .map(mapper::toDTO);
    }

    @Override
    public Page<EventParticipantDTO> getEventParticipantsByUserId(UUID userId, Pageable pageable) {
        return repository.findByUserId(userId, pageable)
                .map(mapper::toDTO);
    }

    @Override
    public Page<EventParticipantDTO> getEventParticipantsByStatusId(Integer statusId, Pageable pageable) {
        return repository.findByStatusId(statusId, pageable)
                .map(mapper::toDTO);
    }

}
