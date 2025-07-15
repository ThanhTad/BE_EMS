package io.event.ems.service;

import io.event.ems.dto.EventParticipantRequestDTO;
import io.event.ems.dto.EventParticipantResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface EventParticipantService {

    EventParticipantResponseDTO registerParticipant(EventParticipantRequestDTO request);

    Page<EventParticipantResponseDTO> getParticipantsByEvent(UUID eventId, Pageable pageable);

    Page<EventParticipantResponseDTO> getEventsByUser(UUID userId, Pageable pageable);

    void unregisterParticipant(UUID participantId);


}
