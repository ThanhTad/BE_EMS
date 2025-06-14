package io.event.ems.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;

@Data
public class EventInfoDTO {

    private UUID id;
    private String title;
    private String coverImageUrl;
    private LocalDateTime startDate;
    private String location;

}
