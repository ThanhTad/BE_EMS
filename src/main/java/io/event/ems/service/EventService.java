package io.event.ems.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import io.event.ems.dto.EventRequestDTO;
import io.event.ems.dto.EventResponseDTO;
import io.event.ems.exception.ResourceNotFoundException;

public interface EventService {

    EventResponseDTO createEvent(EventRequestDTO eventRequestDTO);
    Optional<EventResponseDTO> getEventById(UUID id) throws ResourceNotFoundException;
    Page<EventResponseDTO> getAllEvents(Pageable pageable);
    Page<EventResponseDTO> searchEvents(String keyword, Pageable pageable);
    EventResponseDTO updateEvent(UUID id, EventRequestDTO eventRequestDTO) throws ResourceNotFoundException;
    void deleteEvent(UUID id) throws ResourceNotFoundException;
    Page<EventResponseDTO> getEventByCreatorId(UUID creatorId, Pageable pageable);
    Page<EventResponseDTO> getEventByCategoryId(UUID categoryId, Pageable pageable);
    Page<EventResponseDTO> getEventByStatusId(Integer statusId, Pageable pageable);
    Page<EventResponseDTO>  getEventByStartDateBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

}
