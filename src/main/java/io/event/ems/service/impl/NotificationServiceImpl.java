package io.event.ems.service.impl;

import io.event.ems.dto.NotificationDTO;
import io.event.ems.mapper.NotificationMapper;
import io.event.ems.model.Event;
import io.event.ems.model.Notification;
import io.event.ems.model.User;
import io.event.ems.repository.NotificationRepository;
import io.event.ems.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository repository;
    private final NotificationMapper mapper;

    @Override
    @Cacheable(value = "unreadNotificationCount", key = "#userId")
    public long countUnread(UUID userId) {
        log.debug("Counting unread notifications for user ID: {}", userId);
        return repository.countByUser_IdAndReadFalse(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDTO> getUserNotifications(UUID userId, Pageable pageable) {
        log.debug("Getting notifications for user ID: {}", userId);
        return repository.findByUser_IdAndReadFalseOrderByCreatedAtDesc(userId, pageable)
                .map(mapper::toDTO);
    }

    @Override
    @Transactional
    @CacheEvict(value = "unreadNotificationCount", key = "#userId")
    public void markAsRead(UUID userId, List<UUID> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            return;
        }
        log.debug("Marking notifications for user ID: {} as read: {}", userId, notificationIds);
        int updatedCount = repository.markAsReadForUser(userId, notificationIds);
        log.info("Updated {} notifications for user ID: {}", updatedCount, userId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "unreadNotificationCount", key = "#user.id")
    public void createNotification(User user, String type, String content, Event relatedEvent) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setContent(content);
        notification.setRelatedEvent(relatedEvent);
        notification.setRead(false);

        repository.save(notification);
        log.info("Created notification for user ID: {}", user.getId());
    }

}
