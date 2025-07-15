package io.event.ems.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class EventVenueDTO {

    private UUID venueId;
    private String name;
    private String address;
    private String city;
}
