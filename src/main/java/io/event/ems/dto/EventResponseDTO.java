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
    private VenueDTO venue; // Trả về DTO của Venue
    private Set<CategoryDTO> categories; // Trả về DTO của Category
    private UserSummaryDTO creator; // Trả về DTO tóm tắt của User
    private TicketSelectionModeEnum ticketSelectionMode;
    private SeatMapSummaryDTO seatMap; // Trả về DTO tóm tắt của SeatMap
    private StatusCodeDTO status; // Trả về DTO của StatusCode
    private Boolean isPublic;
    private String coverImageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
