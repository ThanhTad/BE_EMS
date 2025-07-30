package io.event.ems.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateSeatMapRequestDTO {

    @NotBlank(message = "Seat map name cannot be blank")
    @Size(min = 3, max = 255)
    private String name;

    private String description;
    private JsonNode layoutData;

    @NotNull
    @Valid // Kích hoạt validation cho các object trong list
    private List<UpdateSectionRequestDTO> sections;
}
