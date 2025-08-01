package io.event.ems.service.impl;

import io.event.ems.dto.EventCreationDTO;
import io.event.ems.dto.EventResponseDTO;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.mapper.EventMapper;
import io.event.ems.model.*;
import io.event.ems.repository.*;
import io.event.ems.security.CustomUserDetails;
import io.event.ems.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

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

    private static final String EVENT_ENTITY_TYPE = "EVENT";
    private static final String STATUS_PENDING = "PENDING_APPROVAL";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    @Override
    public EventResponseDTO createEvent(EventCreationDTO eventCreationDTO) {
        log.info("Creating event: {}", eventCreationDTO);

        Venue venue = venueRepository.findById(eventCreationDTO.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with id: " + eventCreationDTO.getVenueId()));

        User creator = userRepository.findById(eventCreationDTO.getCreatorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + eventCreationDTO.getCreatorId()));

        String slug = generateUniqueSlug(eventCreationDTO.getTitle());

        StatusCode statusCode = statusCodeRepository.findByEntityTypeAndStatus(EVENT_ENTITY_TYPE, STATUS_PENDING)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Status not found with Entity: EVENT and Status: PENDING_APPROVAL"));

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
    @Transactional
    public EventResponseDTO approveEvent(UUID eventId) {
        log.info("Approving event with id: {}", eventId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        if (!event.getStatus().getStatus().equals(STATUS_PENDING)) {
            throw new IllegalStateException("Event is not in PENDING_APPROVAL status");
        }

        event.setStatus(statusCodeRepository.findByEntityTypeAndStatus(EVENT_ENTITY_TYPE, STATUS_APPROVED)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Status not found with Entity: EVENT and Status: APPROVED")));

        Event updatedEvent = eventRepository.save(event);
        log.info("Approved event with Id: {}", updatedEvent.getId());
        return eventMapper.toResponseDTO(updatedEvent);
    }

    @Override
    @Transactional
    public EventResponseDTO rejectEvent(UUID eventId) {
        log.info("Rejecting event with id: {}", eventId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        if (!event.getStatus().getStatus().equals(STATUS_PENDING)) {
            throw new IllegalStateException("Event is not in PENDING_APPROVAL status");
        }

        event.setStatus(statusCodeRepository.findByEntityTypeAndStatus(EVENT_ENTITY_TYPE, STATUS_REJECTED)
                .orElseThrow(() -> new ResourceNotFoundException("Status 'REJECTED' not found.")));

        Event updatedEvent = eventRepository.save(event);
        log.info("Rejected event with Id: {}", updatedEvent.getId());
        return eventMapper.toResponseDTO(updatedEvent);
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
    public Page<EventResponseDTO> getAllEventsForManagement(Pageable pageable) {
        // Lấy thông tin người dùng đang đăng nhập từ Spring Security Context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails currentUser = (CustomUserDetails) authentication.getPrincipal();

        Page<Event> eventPage;

        // Logic phân quyền cốt lõi nằm ở đây
        if (currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            // Nếu là ADMIN, gọi phương thức findAll của repository
            log.info("Fetching all events for ADMIN user: {}", currentUser.getUsername());
            eventPage = eventRepository.findAll(pageable);
        } else {
            // Nếu là ORGANIZER (hoặc vai trò khác), chỉ lấy các event của họ
            log.info("Fetching events for ORGANIZER user: {}", currentUser.getUsername());
            eventPage = eventRepository.findByCreatorId(currentUser.getId(), pageable);
        }

        // Chuyển đổi Page<Event> sang Page<EventResponseDTO>
        return eventPage.map(eventMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponseDTO> getPublicEvents(Pageable pageable) {
        log.debug("Fetching all public events");
        StatusCode statusCode = statusCodeRepository.findByEntityTypeAndStatus(EVENT_ENTITY_TYPE, STATUS_APPROVED)
                .orElseThrow(() -> new ResourceNotFoundException("Status 'APPROVED' not found."));
        return eventRepository.findByIsPublicTrueAndStatusId(statusCode.getId(), pageable)
                .map(eventMapper::toResponseDTO);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<EventResponseDTO> searchEvents(String keyword, Pageable pageable) {
        log.debug("Searching for events with keyword: {}", keyword);
        if (keyword == null || keyword.trim().isEmpty()) {
            return getPublicEvents(pageable);
        }

        StatusCode approvedStatus = statusCodeRepository.findByEntityTypeAndStatus(EVENT_ENTITY_TYPE, STATUS_APPROVED)
                .orElseThrow(() -> new IllegalStateException("APPROVED status not found in database."));

        String processedQuery = keyword.trim().replaceAll("\\s+", " & ");

        return eventRepository.searchByFullText(processedQuery, approvedStatus.getId(), pageable)
                .map(eventMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponseDTO> searchEventsWithFilters(String keyword,
                                                          List<UUID> categoryIds,
                                                          Integer statusId,
                                                          Boolean isPublic,
                                                          LocalDateTime startDate,
                                                          LocalDateTime endDate,
                                                          Pageable pageable) {
        log.debug("Searching events with filters - keyword: {}, categoryId: {}, statusId: {}, isPublic: {}",
                keyword, categoryIds, statusId, isPublic);

        // Tạo specification động từ các tham số
        Specification<Event> spec = EventSpecification.withDynamicQuery(
                keyword, categoryIds, statusId, isPublic, startDate, endDate);

        return eventRepository.findAll(spec, pageable)
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
            if (!eventRequestDTO.getCategoryIds().isEmpty()) {
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
