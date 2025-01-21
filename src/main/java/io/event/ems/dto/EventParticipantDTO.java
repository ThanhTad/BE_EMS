package io.event.ems.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EventParticipantDTO {

    private UUID id;

    @NotNull(message = "Event Id cannot be null")
    private UUID eventId;

    @NotNull(message = "User Id cannot be null")
    private UUID userId;

    private LocalDateTime registrationDate;

    @NotNull(message = "Status Id cannot be null")
    private Integer statusId;
    
    @Min(value = 0, message = "Additional guests must be at least 0")
    private Integer additionalGuest;


}
