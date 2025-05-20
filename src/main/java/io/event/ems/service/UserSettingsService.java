package io.event.ems.service;

import java.util.UUID;

import io.event.ems.dto.UserSettingsDTO;
import io.event.ems.exception.ResourceNotFoundException;

public interface UserSettingsService {

    UserSettingsDTO getSettings(UUID userId) throws ResourceNotFoundException;

    UserSettingsDTO updateSettings(UUID userId, UserSettingsDTO upd) throws ResourceNotFoundException;

}
