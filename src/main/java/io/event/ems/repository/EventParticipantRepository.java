package io.event.ems.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.event.ems.model.Event;
import io.event.ems.model.EventParticipant;
import io.event.ems.model.User;

@Repository
public interface EventParticipantRepository extends JpaRepository<EventParticipant, UUID> {

    Page<EventParticipant> findByEventId(UUID eventId, Pageable pageable);
    Page<EventParticipant> findByUserId(UUID userId, Pageable pageable);
    Page<EventParticipant> findByStatusId(Integer statusId, Pageable pageable);
    boolean existsByEventAndUser(Event event, User user);

}
