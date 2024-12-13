package io.event.ems.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.event.ems.model.StatusCode;

@Repository
public interface StatusCodeRepository extends JpaRepository<StatusCode, Integer> {

    Optional<StatusCode> findByEntityTypeAndStatus(String entityType, String status);

}
