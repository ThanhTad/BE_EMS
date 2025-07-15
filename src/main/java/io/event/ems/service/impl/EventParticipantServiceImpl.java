package io.event.ems.service.impl;

import io.event.ems.dto.EventParticipantRequestDTO;
import io.event.ems.dto.EventParticipantResponseDTO;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EventParticipantServiceImpl implements EventParticipantService {

    private final EventParticipantRepository repository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final StatusCodeRepository statusCodeRepository;
    private final EventParticipantMapper mapper;

    @Override
    public EventParticipantResponseDTO registerParticipant(EventParticipantRequestDTO request) {
        repository.findByEvent_IdAndUser_Id(request.getEventId(), request.getUserId())
                .ifPresent(p -> {
                    throw new DataIntegrityViolationException("User is already registered for event: " + request.getEventId());
                });
        log.info("Registering participant for event: {}", request.getEventId());

        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + request.getEventId()));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        StatusCode statusCode = statusCodeRepository.findById(request.getStatusId())
                .orElseThrow(() -> new ResourceNotFoundException("Status not found with id: " + request.getStatusId()));

        EventParticipant participant = new EventParticipant();
        participant.setEvent(event);
        participant.setUser(user);
        participant.setStatus(statusCode);
        participant.setAdditionalGuests(request.getAdditionalGuests());

        EventParticipant savedParticipant = repository.save(participant);
        return mapper.toDTO(savedParticipant);
    }

    @Override
    public Page<EventParticipantResponseDTO> getParticipantsByEvent(UUID eventId, Pageable pageable) {
        Page<EventParticipant> participants = repository.findByEvent_Id(eventId, pageable);
        return participants.map(mapper::toDTO);
    }

    @Override
    public Page<EventParticipantResponseDTO> getEventsByUser(UUID userId, Pageable pageable) {
        Page<EventParticipant> participants = repository.findByUser_Id(userId, pageable);
        return participants.map(mapper::toDTO);
    }

    @Override
    public void unregisterParticipant(UUID participantId) {
        if (!repository.existsById(participantId)) {
            throw new ResourceNotFoundException("Participant not found with id: " + participantId);
        }
        repository.deleteById(participantId);
    }
}
