package io.event.ems.dto;

import java.util.UUID;

public record PaymentResponseDTO(
                UUID transactionId,
                String paymentUrl,
                String message) {

}
