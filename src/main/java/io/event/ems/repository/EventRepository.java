package io.event.ems.repository;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.event.ems.model.Event;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    Page<Event> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Event> findByCreatorId(UUID id, Pageable pageable);

    Page<Event> findByCategories_Id(UUID id, Pageable pageable);

    Page<Event> findByStatusId(Integer id, Pageable pageable);

    Page<Event> findByStartDateBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE " +
            "LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(e.location) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Event> searchEvents(@Param("keyword") String keyword, Pageable pageable);

}
