package io.event.ems.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.event.ems.dto.EventRequestDTO;
import io.event.ems.dto.EventResponseDTO;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.mapper.EventMapper;
import io.event.ems.model.Category;
import io.event.ems.model.Event;
import io.event.ems.model.StatusCode;
import io.event.ems.model.User;
import io.event.ems.repository.CategoryRepository;
import io.event.ems.repository.EventRepository;
import io.event.ems.repository.StatusCodeRepository;
import io.event.ems.repository.UserRepository;
import io.event.ems.service.EventService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final CategoryRepository categoryRepository;
    private final StatusCodeRepository statusCodeRepository;
    private final UserRepository userRepository;

    @Override
    public EventResponseDTO createEvent(EventRequestDTO eventRequestDTO) {
        User creator = userRepository.findById(eventRequestDTO.getCreatorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + eventRequestDTO.getCreatorId()));
        StatusCode statusCode = statusCodeRepository.findByEntityTypeAndStatus("EVENT", "PUBLISHED")
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Status not found with Entity: EVENT and Status: PUBLISHED"));

        Event event = eventMapper.toEntity(eventRequestDTO);
        event.setCreator(creator);
        event.setStatus(statusCode);
        event.setCurrentParticipants(0);

        Set<Category> categories = categoryRepository.findAllById(eventRequestDTO.getCategoryIds())
                .stream().collect(Collectors.toSet());

        if (categories.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No valid categories found for ids: " + eventRequestDTO.getCategoryIds());
        }
        event.setCategories(categories);
        Event savedEvent = eventRepository.save(event);
        return eventMapper.toDTO(savedEvent);
    }

    @Override
    public Optional<EventResponseDTO> getEventById(UUID id) throws ResourceNotFoundException {
        return eventRepository.findById(id)
                .map(eventMapper::toDTO);
    }

    @Override
    public Page<EventResponseDTO> getAllEvents(Pageable pageable) {
        return eventRepository.findAll(pageable)
                .map(eventMapper::toDTO);
    }

    @Override
    public Page<EventResponseDTO> searchEvents(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllEvents(pageable);
        }

        return eventRepository.searchEvents(keyword, pageable)
                .map(eventMapper::toDTO);
    }

    @Override
    public EventResponseDTO updateEvent(UUID id, EventRequestDTO eventRequestDTO) throws ResourceNotFoundException {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + id));

        Set<Category> categories = categoryRepository.findAllById(eventRequestDTO.getCategoryIds())
                .stream().collect(Collectors.toSet());
        if (categories.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No valid categories found for ids: " + eventRequestDTO.getCategoryIds());
        }
        event.setCategories(categories);

        eventMapper.updateEventFromDTO(eventRequestDTO, event);
        Event updatedEvent = eventRepository.save(event);
        return eventMapper.toDTO(updatedEvent);
    }

    @Override
    public void deleteEvent(UUID id) throws ResourceNotFoundException {
        if (!eventRepository.existsById(id)) {
            throw new ResourceNotFoundException("Event not found with id: " + id);
        }
        eventRepository.deleteById(id);
    }

    @Override
    public Page<EventResponseDTO> getEventByCreatorId(UUID creatorId, Pageable pageable) {
        return eventRepository.findByCreatorId(creatorId, pageable)
                .map(eventMapper::toDTO);
    }

    @Override
    public Page<EventResponseDTO> findByCategories_Id(UUID categoryId, Pageable pageable) {
        return eventRepository.findByCategories_Id(categoryId, pageable)
                .map(eventMapper::toDTO);
    }

    @Override
    public Page<EventResponseDTO> getEventByStatusId(Integer statusId, Pageable pageable) {
        return eventRepository.findByStatusId(statusId, pageable)
                .map(eventMapper::toDTO);
    }

    @Override
    public Page<EventResponseDTO> getEventByStartDateBetween(LocalDateTime start, LocalDateTime end,
            Pageable pageable) {
        return eventRepository.findByStartDateBetween(start, end, pageable)
                .map(eventMapper::toDTO);
    }

}
