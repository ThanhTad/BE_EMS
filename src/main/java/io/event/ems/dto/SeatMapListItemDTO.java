package io.event.ems.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class SeatMapListItemDTO {

    private UUID id;
    private String name;
    private String description;
    private int sectionCount;
}
