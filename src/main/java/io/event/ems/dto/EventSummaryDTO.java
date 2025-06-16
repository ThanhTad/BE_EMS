package io.event.ems.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class EventSummaryDTO {

    private UUID id;
    private String title;
    private String slug;
    private LocalDateTime startDate;
    private String venueName;
    private String coverImageUrl;
}
