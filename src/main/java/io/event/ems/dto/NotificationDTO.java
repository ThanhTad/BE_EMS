package io.event.ems.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder // Sử dụng Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {

    private UUID id;
    private String type;
    private String content;
    private boolean read;
    private LocalDateTime createdAt;

    private RelatedEventInfo relatedEvent; // Thông tin về sự kiện liên quan

    // Lớp con để chứa thông tin cần thiết từ Event
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatedEventInfo {
        private UUID id;
        private String title;
        private String slug;
        private String coverImageUrl;
    }
}