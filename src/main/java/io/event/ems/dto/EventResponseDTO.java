package io.event.ems.dto;

import io.event.ems.model.TicketSelectionModeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventResponseDTO {

    private UUID id;
    private String title;
    private String slug;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private VenueDTO venue;
    private Set<CategoryDTO> categories;
    private UserSummaryDTO creator;
    private TicketSelectionModeEnum ticketSelectionMode;
    private SeatMapSummaryDTO seatMap;
    private StatusCodeDTO status;
    private Boolean isPublic;
    private String coverImageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
