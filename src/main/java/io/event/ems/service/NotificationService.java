package io.event.ems.service;

import io.event.ems.dto.NotificationDTO;
import io.event.ems.model.Event;
import io.event.ems.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    long countUnread(UUID userId);

    Page<NotificationDTO> getUserNotifications(UUID userId, Pageable pageable);

    void markAsRead(UUID userId, List<UUID> notificationIds);

    void createNotification(User user, String type, String content, Event relatedEvent);

}

