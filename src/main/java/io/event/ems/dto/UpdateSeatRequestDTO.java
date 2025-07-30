package io.event.ems.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateSeatRequestDTO {

    private UUID id; // Có thể null nếu là ghế mới được tạo ở Frontend

    @NotBlank
    private String rowLabel;

    @NotBlank
    private String seatNumber;

    private String seatType;
    private JsonNode coordinates;
}
