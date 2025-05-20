package io.event.ems.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import io.event.ems.dto.NotificationDTO;
import io.event.ems.model.NotificationType;

public interface NotificationService {

    long countUnread(UUID userId);

    Page<NotificationDTO> getUnread(UUID userId, Pageable pageable);

    void markAsRead(UUID userId, List<Long> ids);

    void create(UUID userId, NotificationType type, String title, String message, String url);

}
