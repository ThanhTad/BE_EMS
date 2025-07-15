package io.event.ems.service.specialized;

import io.event.ems.model.Event;
import io.event.ems.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventSearchService {

    private final EventRepository eventRepository;
    private static final int MAX_EVENTS_TO_SHOW = 5;

    @Cacheable(value = "events", key = "#eventName")
    public List<Event> findRelevantEvents(String eventName) {
        log.debug("Finding relevant events for: {}", eventName);

        // Try exact match first
        Optional<Event> exactMatch = eventRepository.findByTitleIgnoreCase(eventName);
        if (exactMatch.isPresent()) {
            return Collections.singletonList(exactMatch.get());
        }

        // Fall back to partial match with pagination
        Page<Event> eventPage = eventRepository.findPublishedByTitleContainingIgnoreCase(
                eventName, PageRequest.of(0, MAX_EVENTS_TO_SHOW));
        return eventPage.getContent();
    }
}
