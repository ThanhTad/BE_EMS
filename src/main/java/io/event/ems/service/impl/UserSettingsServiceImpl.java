package io.event.ems.service.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;

import io.event.ems.dto.UserSettingsDTO;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.mapper.UserSettingsMapper;
import io.event.ems.model.User;
import io.event.ems.model.UserSettings;
import io.event.ems.repository.UserRepository;
import io.event.ems.repository.UserSettingsRepository;
import io.event.ems.service.UserSettingsService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSettingsServiceImpl implements UserSettingsService {

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final UserSettingsMapper mapper;

    @Override
    public UserSettingsDTO getSettings(UUID userId) {
        log.debug("Fetching settings for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", userId);
                    return new ResourceNotFoundException("User not found with id: " + userId);
                });

        UserSettings settings = user.getSettings();

        return mapper.toDTO(settings);
    }

    @Override
    @Transactional
    public UserSettingsDTO updateSettings(UUID userId, UserSettingsDTO upd) {
        log.debug("Updating settings for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", userId);
                    return new ResourceNotFoundException("User not found with id: " + userId);
                });

        UserSettings existSettings = user.getSettings();

        mapper.updateFromDto(upd, existSettings);

        UserSettings updated = userSettingsRepository.save(existSettings);
        log.info("Successfully updated settings for user ID: {}", userId);

        return mapper.toDTO(updated);
    }

}
