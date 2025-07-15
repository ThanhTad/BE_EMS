package io.event.ems.service;

import java.math.BigDecimal;

public interface PaymentGatewayService {

    String processPayment(String paymentToken, BigDecimal amount);
}
