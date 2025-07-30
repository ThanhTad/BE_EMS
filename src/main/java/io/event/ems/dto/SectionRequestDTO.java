package io.event.ems.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SectionRequestDTO {

    @NotBlank(message = "Section name cannot be blank")
    private String name;

    @NotNull(message = "Capacity is required")
    @Min(value = 0, message = "Capacity must be a non-negative number")
    private Integer capacity;

    @NotNull(message = "Layout data is required")
    private JsonNode layoutData;
}
