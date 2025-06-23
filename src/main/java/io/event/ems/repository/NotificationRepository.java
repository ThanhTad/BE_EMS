package io.event.ems.repository;

import io.event.ems.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    long countByUser_IdAndReadFalse(UUID userId);

    Page<Notification> findByUser_IdAndReadFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Modifying // Bắt buộc cho các câu lệnh UPDATE/DELETE
    @Query("UPDATE Notification n SET n.read = true WHERE n.user.id = :userId AND n.id IN :notificationIds AND n.read = false")
    int markAsReadForUser(UUID userId, List<UUID> notificationIds);
}
