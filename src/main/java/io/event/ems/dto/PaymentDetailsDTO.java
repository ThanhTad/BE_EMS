package io.event.ems.dto;

import lombok.Data;

@Data
public class PaymentDetailsDTO {

    private String paymentMethod;
    private String paymentToken;
    private Double amount;
}
