package io.event.ems.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class VenueDTO {

    private UUID id;
    private String name;
    private String address;
}

