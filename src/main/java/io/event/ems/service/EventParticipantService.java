package io.event.ems.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import io.event.ems.dto.EventParticipantDTO;
import io.event.ems.exception.ResourceNotFoundException;

public interface EventParticipantService {

    Page<EventParticipantDTO> getAllEventParticipant(Pageable pageable);

    Optional<EventParticipantDTO> getEventParticipantById(UUID id) throws ResourceNotFoundException;

    EventParticipantDTO createEventParticipant(EventParticipantDTO eventParticipantDTO) throws ResourceNotFoundException;

    EventParticipantDTO updateEventParticipant(UUID id, EventParticipantDTO eventParticipantDTO) throws ResourceNotFoundException;

    void deleteEventParticipant(UUID id) throws ResourceNotFoundException;

    Page<EventParticipantDTO> getEventParticipantsByEventId(UUID eventId, Pageable pageable);
    Page<EventParticipantDTO> getEventParticipantsByUserId(UUID userId, Pageable pageable);
    Page<EventParticipantDTO> getEventParticipantsByStatusId(Integer statusId, Pageable pageable);
    
}
