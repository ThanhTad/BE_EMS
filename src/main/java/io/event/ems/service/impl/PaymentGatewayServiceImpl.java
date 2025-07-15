package io.event.ems.service.impl;

import io.event.ems.service.PaymentGatewayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
public class PaymentGatewayServiceImpl implements PaymentGatewayService {

    @Override
    public String processPayment(String paymentToken, BigDecimal amount) {
        log.debug("Processing payment for token: {} and amount: {}", paymentToken, amount);

        if (paymentToken == null || paymentToken.isBlank()) {
            throw new IllegalArgumentException("Payment token is invalid");
        }

        log.info("Payment processed successfully for token: {} and amount: {}", paymentToken, amount);
        return "txn_" + UUID.randomUUID().toString().replace("-", "");
    }
}
