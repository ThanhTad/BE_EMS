package io.event.ems.service.payment;

import io.event.ems.dto.PaymentCreationResultDTO;
import io.event.ems.model.TicketPurchase;

import java.util.Map;

public interface PaymentGateway {

    String getProviderName();

    PaymentCreationResultDTO createPaymentUrl(TicketPurchase purchase, String ipAddress);

    boolean handlePaymentReturn(Map<String, String> params);
}
