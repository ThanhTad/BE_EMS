package io.event.ems.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class CheckoutRequestDTO {

    private UUID holdId;
    private UUID userId;
    private String paymentToken;
}
