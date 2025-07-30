package io.event.ems.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class UpdateSectionRequestDTO {

    private UUID id; // Có thể null nếu là section mới được tạo ở Frontend

    @NotBlank
    private String name;

    private JsonNode layoutData;

    @Valid
    private List<UpdateSeatRequestDTO> seats;
}
