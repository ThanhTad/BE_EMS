package io.event.ems.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HoldDetailsResponseDTO {
    private UUID holdId;
    private UUID eventId;
    private LocalDateTime expiresAt;
    private TicketHoldRequestDTO request;
}
