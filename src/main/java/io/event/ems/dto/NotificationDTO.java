package io.event.ems.dto;

import java.time.LocalDateTime;

import io.event.ems.model.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {

    private Long id;
    private NotificationType type;
    private String title;
    private String message;
    private String url;
    private Boolean read;
    private LocalDateTime createdAt;
}
