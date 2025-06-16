package io.event.ems.repository;

import io.event.ems.model.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VenueRepository extends JpaRepository<Venue, UUID> {
    boolean existsByName(String name);

    Optional<Venue> findByName(String name);
}
