package io.event.ems.dto;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestDTO {

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

    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;

    private String address;

    @NotEmpty(message = "Phải chọn ít nhất 1 category")
    private Set<@NotNull UUID> categoryIds;

    @NotNull(message = "Creator ID cannot be null")
    private UUID creatorId;

    @Positive(message = "Max participants must be a positive number")
    private Integer maxParticipants;

    private LocalDateTime registrationStartDate;
    private LocalDateTime registrationEndDate;
    private Boolean isPublic = true;
    private String coverImageUrl;
    private Double latitude;
    private Double longitude;

}
