package io.event.ems.service;

import io.event.ems.dto.EventCreationDTO;
import io.event.ems.dto.EventResponseDTO;
import io.event.ems.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface EventService {

    EventResponseDTO createEvent(EventCreationDTO eventCreationDTO);

    Optional<EventResponseDTO> getEventById(UUID id);

    Optional<EventResponseDTO> getEventBySlug(String slug);

    Page<EventResponseDTO> getAllEvents(Pageable pageable);

    Page<EventResponseDTO> getPublicEvents(Pageable pageable);

    Page<EventResponseDTO> searchEvents(String keyword, Pageable pageable);

    Page<EventResponseDTO> searchEventsWithFilters(String keyword,
                                                   UUID categoryId,
                                                   Integer statusId,
                                                   Boolean isPublic,
                                                   LocalDateTime startDate,
                                                   LocalDateTime endDate,
                                                   Pageable pageable);

    EventResponseDTO updateEvent(UUID id, EventCreationDTO eventCreationDTO) throws ResourceNotFoundException;

    void deleteEvent(UUID id) throws ResourceNotFoundException;

    Page<EventResponseDTO> getEventByCreatorId(UUID creatorId, Pageable pageable);

    Page<EventResponseDTO> findByCategories_Id(UUID categoryId, Pageable pageable);

    Page<EventResponseDTO> getEventByStatusId(Integer statusId, Pageable pageable);

    Page<EventResponseDTO> getEventByStartDateBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    boolean isSlugAvailable(String slug);

    String generateUniqueSlug(String title);
}
