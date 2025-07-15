package io.event.ems.dto;

import io.event.ems.model.TicketSelectionModeEnum;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class EventTicketingResponseDTO {

    private UUID eventId;
    private String eventTitle;
    private String slug;
    private String eventDescription;
    private String coverImageUrl;
    private LocalDateTime eventStartDate;
    private LocalDateTime eventEndDate;
    private Boolean isPublic;
    private EventVenueDTO venue;
    private EventCreatorDTO creator;

    // Ticketing Info
    private TicketSelectionModeEnum ticketSelectionMode;
    private Object ticketingData;
}
