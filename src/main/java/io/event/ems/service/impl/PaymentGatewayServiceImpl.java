package io.event.ems.service.impl;

import io.event.ems.dto.PaymentCreationResultDTO;
import io.event.ems.model.TicketPurchase;
import io.event.ems.service.PaymentGatewayService;
import io.event.ems.service.payment.PaymentGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PaymentGatewayServiceImpl implements PaymentGatewayService {

    private final Map<String, PaymentGateway> gateways;


    public PaymentGatewayServiceImpl(List<PaymentGateway> gatewayList) {
        this.gateways = gatewayList.stream()
                .collect(Collectors.toMap(
                        gateway -> gateway.getProviderName().toUpperCase(),
                        Function.identity()
                ));
        log.info("Initialized PaymentGatewayService with available gateways: {}", gateways.keySet());
    }

    @Override
    public String processPayment(String paymentToken, BigDecimal amount) {
        log.debug("Processing payment for token: {} and amount: {}", paymentToken, amount);

        if (paymentToken == null || paymentToken.isBlank()) {
            throw new IllegalArgumentException("Payment token is invalid");
        }

        log.info("Payment processed successfully for token: {} and amount: {}", paymentToken, amount);
        return "txn_" + UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public PaymentCreationResultDTO createPayment(String provider, TicketPurchase purchase, String ipAddress) {
        log.debug("Dispatching payment creation request to provider: {}", provider);
        PaymentGateway gateway = getGateway(provider);
        return gateway.createPaymentUrl(purchase, ipAddress);
    }

    @Override
    public boolean verifyPayment(String provider, Map<String, String> params) {
        log.debug("Dispatching payment verification request to provider: {}", provider);
        PaymentGateway gateway = getGateway(provider);
        return gateway.handlePaymentReturn(params);
    }

    private PaymentGateway getGateway(String provider) {
        PaymentGateway gateway = gateways.get(provider.toUpperCase());
        if (gateway == null) {
            log.error("Unsupported payment provider requested: {}. Available providers: {}", provider, gateways.keySet());
            throw new IllegalArgumentException("Unsupported payment provider: " + provider);
        }
        return gateway;
    }
}
