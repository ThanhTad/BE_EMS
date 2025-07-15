package io.event.ems.model;

import io.event.ems.dto.TicketHoldRequestDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HoldData {

    private UUID holdId;
    private UUID eventId;
    private UUID userId;
    private TicketHoldRequestDTO request;
    private LocalDateTime expiresAt;
}
