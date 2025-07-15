package io.event.ems.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for Venue entity
 */
@Data
@NoArgsConstructor
public class VenueDTO {

    private UUID id;
    private String name;
    private String address;
    private String city;
    private String country;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}