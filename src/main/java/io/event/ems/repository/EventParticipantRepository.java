package io.event.ems.repository;

import io.event.ems.model.EventParticipant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventParticipantRepository extends JpaRepository<EventParticipant, UUID> {

    Page<EventParticipant> findByEvent_Id(UUID eventId, Pageable pageable);

    Page<EventParticipant> findByUser_Id(UUID userId, Pageable pageable);

    Optional<EventParticipant> findByEvent_IdAndUser_Id(UUID eventId, UUID userId);


}
