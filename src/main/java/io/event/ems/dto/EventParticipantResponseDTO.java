package io.event.ems.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class EventParticipantResponseDTO {

    private UUID id;
    private LocalDateTime registrationDate;
    private Integer additionalGuests;

    private SimpleEventDTO event;
    private SimpleUserDTO user;
    private SimpleStatusDTO status;

    @Data
    public static class SimpleEventDTO {
        private UUID id;
        private String title;
    }

    @Data
    public static class SimpleUserDTO {
        private UUID id;
        private String username;
        private String fullName;
    }

    @Data
    public static class SimpleStatusDTO {
        private Integer id;
        private String status;
        private String description;
    }
}

