package io.event.ems.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class ZoneLayoutDTO {

    private String name;
    private int capacity;
    private JsonNode layoutData;
}
