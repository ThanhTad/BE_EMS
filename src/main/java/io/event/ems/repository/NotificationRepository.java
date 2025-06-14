package io.event.ems.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import io.event.ems.model.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    long countByUserIdAndReadFalse(UUID userId);

    Page<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);

}
