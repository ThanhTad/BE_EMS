package io.event.ems.dto;

import java.util.UUID;

public record PaymentResponseDTO(
        UUID purchaseId,
        String paymentUrl,
        String message) {

}
