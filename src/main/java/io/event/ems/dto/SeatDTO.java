package io.event.ems.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class SeatDTO {

    private UUID seatId;
    private String rowLabel;
    private String seatNumber;
    private String seatType;
    private String status;
    private BigDecimal price;
    private String ticketTypeName;
    private UUID ticketId;
    private JsonNode coordinates;
    private LocalDateTime heldUntil;
}
