package io.event.ems.service;

import io.event.ems.dto.PaymentDetailsDTO;
import io.event.ems.dto.TicketPurchaseConfirmationDTO;
import io.event.ems.model.HoldData;

public interface OrderProcessingService {

    TicketPurchaseConfirmationDTO finalizePurchase(HoldData holdData, PaymentDetailsDTO paymentDetails);
}
