package io.event.ems.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import io.event.ems.model.StatusCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventResponseDTO {

    private UUID id;
    private String title;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String location;
    private String address;
    private CategoryDTO category;
    private UserResponseDTO creator;
    private Integer maxParticipants;
    private Integer currentParticipants;
    private StatusCode status;
    private LocalDateTime createdAt;
    private LocalDateTime registrationStartDate;
    private LocalDateTime registrationEndDate;
    private Boolean isPublic;
    private String coverImageUrl;
    private Double latitude;
    private Double longitude;


}
