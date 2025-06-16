package io.event.ems.service.impl;

import io.event.ems.dto.EventCreationDTO;
import io.event.ems.dto.EventResponseDTO;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.mapper.EventMapper;
import io.event.ems.model.*;
import io.event.ems.repository.*;
import io.event.ems.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final CategoryRepository categoryRepository;
    private final StatusCodeRepository statusCodeRepository;
    private final UserRepository userRepository;
    private final VenueRepository venueRepository;
    private final SeatMapRepository seatMapRepository;

    @Override
    public EventResponseDTO createEvent(EventCreationDTO eventCreationDTO) {
        log.info("Creating event: {}", eventCreationDTO);

        Venue venue = venueRepository.findById(eventCreationDTO.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with id: " + eventCreationDTO.getVenueId()));

        User creator = userRepository.findById(eventCreationDTO.getCreatorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + eventCreationDTO.getCreatorId()));

        String slug = generateUniqueSlug(eventCreationDTO.getTitle());

        StatusCode statusCode = statusCodeRepository.findByEntityTypeAndStatus("EVENT", "PUBLISHED")
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Status not found with Entity: EVENT and Status: PUBLISHED"));

        Event event = eventMapper.toEntity(eventCreationDTO);
        event.setSlug(slug);
        event.setVenue(venue);
        event.setCreator(creator);
        event.setStatus(statusCode);

        if (eventCreationDTO.getCategoryIds() != null && !eventCreationDTO.getCategoryIds().isEmpty()) {
            Set<Category> categories = new HashSet<>();
            for (UUID categoryId : eventCreationDTO.getCategoryIds()) {
                Category category = categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
                categories.add(category);
            }
            event.setCategories(categories);
        }

        if (eventCreationDTO.getSeatMapId() != null) {
            SeatMap seatMap = seatMapRepository.findById(eventCreationDTO.getSeatMapId())
                    .orElseThrow(() -> new ResourceNotFoundException("SeatMap not found with id: " + eventCreationDTO.getSeatMapId()));
            event.setSeatMap(seatMap);
        }

        Event savedEvent = eventRepository.save(event);
        return eventMapper.toResponseDTO(savedEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EventResponseDTO> getEventById(UUID id) throws ResourceNotFoundException {
        log.debug("Fetching event with id: {}", id);
        return eventRepository.findById(id)
                .map(eventMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EventResponseDTO> getEventBySlug(String slug) throws ResourceNotFoundException {
        log.debug("Fetching event with slug: {}", slug);
        return eventRepository.findBySlug(slug)
                .map(eventMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponseDTO> getAllEvents(Pageable pageable) {
        log.debug("Fetching all events");
        return eventRepository.findAll(pageable)
                .map(eventMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponseDTO> getPublicEvents(Pageable pageable) {
        log.debug("Fetching all public events");
        return eventRepository.findByIsPublicTrue(pageable)
                .map(eventMapper::toResponseDTO);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<EventResponseDTO> searchEvents(String keyword, Pageable pageable) {
        log.debug("Searching for events with keyword: {}", keyword);
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllEvents(pageable);
        }

        return eventRepository.searchEvents(keyword, pageable)
                .map(eventMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponseDTO> searchEventsWithFilters(String keyword,
                                                          UUID categoryId,
                                                          Integer statusId,
                                                          Boolean isPublic,
                                                          LocalDateTime startDate,
                                                          LocalDateTime endDate,
                                                          Pageable pageable) {
        log.debug("Searching events with filters - keyword: {}, categoryId: {}, statusId: {}, isPublic: {}",
                keyword, categoryId, statusId, isPublic);

        return eventRepository.searchEventsWithFilters(
                        keyword, categoryId, statusId, isPublic, startDate, endDate, pageable)
                .map(eventMapper::toResponseDTO);
    }


    @Override
    public EventResponseDTO updateEvent(UUID id, EventCreationDTO eventRequestDTO) throws ResourceNotFoundException {
        log.debug("Updating event with id: {}", id);

        Event existingEvent = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + id));

        if (!existingEvent.getVenue().getId().equals(eventRequestDTO.getVenueId())) {
            Venue venue = venueRepository.findById(eventRequestDTO.getVenueId())
                    .orElseThrow(() -> new ResourceNotFoundException("Venue not found with id: " + eventRequestDTO.getVenueId()));
            existingEvent.setVenue(venue);
        }

        if (!existingEvent.getTitle().equals(eventRequestDTO.getTitle())) {
            String slug = generateUniqueSlug(eventRequestDTO.getTitle());
            existingEvent.setSlug(slug);
            existingEvent.setTitle(eventRequestDTO.getTitle());
        }

        eventMapper.updateEntityFromDTO(eventRequestDTO, existingEvent);

        if (eventRequestDTO.getStatusId() != null) {
            StatusCode statusCode = statusCodeRepository.findById(eventRequestDTO.getStatusId())
                    .orElseThrow(() -> new ResourceNotFoundException("Status not found with id: " + eventRequestDTO.getStatusId()));
            existingEvent.setStatus(statusCode);
        }

        if (eventRequestDTO.getCategoryIds() != null) {
            existingEvent.getCategories().clear();
            if (eventRequestDTO.getCategoryIds().isEmpty()) {
                Set<Category> categories = new HashSet<>();
                for (UUID categoryId : eventRequestDTO.getCategoryIds()) {
                    Category category = categoryRepository.findById(categoryId)
                            .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
                    categories.add(category);
                }
                existingEvent.setCategories(categories);
            }
        }

        // Cập nhật TicketSelectionMode và SeatMap
        if (eventRequestDTO.getTicketSelectionMode() != null) {
            existingEvent.setTicketSelectionMode(eventRequestDTO.getTicketSelectionMode());
            switch (eventRequestDTO.getTicketSelectionMode()) {
                case RESERVED_SEATING:
                case ZONED_ADMISSION:
                    if (eventRequestDTO.getSeatMapId() == null) {
                        throw new IllegalArgumentException("SeatMapId is required for " + eventRequestDTO.getTicketSelectionMode());
                    }
                    SeatMap seatMap = seatMapRepository.findById(eventRequestDTO.getSeatMapId())
                            .orElseThrow(() -> new ResourceNotFoundException("SeatMap not found with id: " + eventRequestDTO.getSeatMapId()));
                    existingEvent.setSeatMap(seatMap);
                    break;
                case GENERAL_ADMISSION:
                    existingEvent.setSeatMap(null);
                    break;
            }
        }

        Event updatedEvent = eventRepository.save(existingEvent);
        log.debug("Updated event with Id: {}", updatedEvent.getId());
        return eventMapper.toResponseDTO(updatedEvent);
    }

    @Override
    public void deleteEvent(UUID id) throws ResourceNotFoundException {
        log.debug("Deleting event with id: {}", id);
        if (!eventRepository.existsById(id)) {
            throw new ResourceNotFoundException("Event not found with id: " + id);
        }
        eventRepository.deleteById(id);
        log.info("Event deleted successfully with id: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponseDTO> getEventByCreatorId(UUID creatorId, Pageable pageable) {
        log.debug("Fetching events created by creator with id: {}", creatorId);
        return eventRepository.findByCreatorId(creatorId, pageable)
                .map(eventMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponseDTO> findByCategories_Id(UUID categoryId, Pageable pageable) {
        log.debug("Fetching events with category with id: {}", categoryId);
        return eventRepository.findByCategories_Id(categoryId, pageable)
                .map(eventMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponseDTO> getEventByStatusId(Integer statusId, Pageable pageable) {
        log.debug("Fetching events with statusId: {}", statusId);
        return eventRepository.findByStatusId(statusId, pageable)
                .map(eventMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponseDTO> getEventByStartDateBetween(LocalDateTime start, LocalDateTime end,
                                                             Pageable pageable) {
        log.debug("Fetching events with start date between: {} and {}", start, end);
        return eventRepository.findByStartDateBetween(start, end, pageable)
                .map(eventMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSlugAvailable(String slug) {
        return !eventRepository.existsBySlug(slug);
    }

    @Override
    public String generateUniqueSlug(String title) {
        String baseSlug = title.toLowerCase().replaceAll("[^a-z0-9\\-]", "-").trim();

        if (baseSlug.length() > 60) {
            baseSlug = baseSlug.substring(0, 60);
        }

        String slug = baseSlug;
        int counter = 1;

        while (!isSlugAvailable(slug)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }
        return slug;
    }
}
