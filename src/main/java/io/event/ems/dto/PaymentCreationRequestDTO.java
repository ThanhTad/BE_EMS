package io.event.ems.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class PaymentCreationRequestDTO {

    private UUID holdId;
    private String paymentMethod;
}
