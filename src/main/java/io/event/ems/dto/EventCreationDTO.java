package io.event.ems.dto;

import io.event.ems.model.TicketSelectionModeEnum;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventCreationDTO {

    @NotBlank(message = "Title cannot be blank")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    private String description;

    @NotNull(message = "Start date cannot be null")
    @FutureOrPresent(message = "Start date must be in the present or future")
    private LocalDateTime startDate;

    @NotNull(message = "End date cannot be null")
    @Future(message = "End date must be in the future")
    private LocalDateTime endDate;

    @NotNull(message = "Venue ID cannot be null")
    private UUID venueId;

    @NotEmpty(message = "Phải chọn ít nhất 1 category")
    private Set<@NotNull UUID> categoryIds;

    @NotNull(message = "Creator ID cannot be null")
    private UUID creatorId;

    private TicketSelectionModeEnum ticketSelectionMode;
    private UUID seatMapId;

    @NotNull(message = "Status ID cannot be null")
    private Integer statusId;
    
    private Boolean isPublic;
    private String coverImageUrl;

}
