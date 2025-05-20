package io.event.ems.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.event.ems.model.UserSettings;

public interface UserSettingsRepository extends JpaRepository<UserSettings, UUID> {

}
