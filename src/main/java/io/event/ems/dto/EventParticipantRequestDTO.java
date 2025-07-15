package io.event.ems.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class EventParticipantRequestDTO {

    @NotNull(message = "Event ID cannot be null")
    private UUID eventId;

    @NotNull(message = "User ID cannot be null")
    private UUID userId;

    @NotNull(message = "Status ID cannot be null")
    private Integer statusId;

    @Min(value = 0, message = "Additional guests must be at least 0")
    private Integer additionalGuests;
}
