package io.event.ems.dto;

import java.time.LocalDateTime;

import io.event.ems.model.ThemeOption;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingsDTO {

    private Boolean receiveEventReminders;
    private Boolean receiveNewEventNotifications;
    private Boolean receivePromotionalEmails;
    private ThemeOption theme;
    private Boolean syncWithGoogleCalendar;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
