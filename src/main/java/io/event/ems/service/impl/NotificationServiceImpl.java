package io.event.ems.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.event.ems.dto.NotificationDTO;
import io.event.ems.mapper.NotificationMapper;
import io.event.ems.model.Notification;
import io.event.ems.model.NotificationType;
import io.event.ems.repository.NotificationRepository;
import io.event.ems.service.NotificationService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository repository;
    private final NotificationMapper mapper;

    @Override
    @Cacheable(value = "notifCount", key = "#userId")
    public long countUnread(UUID userId) {
        return repository.countByUserIdAndReadFalse(userId);
    }

    @Override
    public Page<NotificationDTO> getUnread(UUID userId, Pageable pageable) {
        return repository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId, pageable)
                .map(mapper::toDTO);
    }

    @Override
    @Transactional
    @CacheEvict(value = "notifCount", key = "#userId")
    public void markAsRead(UUID userId, List<Long> ids) {
        repository.findAllById(ids).stream()
                .filter(n -> n.getUserId().equals(userId) && !n.isRead())
                .forEach(n -> n.setRead(true));
    }

    @Override
    @CacheEvict(value = "notifCount", key = "#userId")
    public void create(UUID userId, NotificationType type, String title, String message, String url) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setUrl(url);
        repository.save(n);
    }

}
