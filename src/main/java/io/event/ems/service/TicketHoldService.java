package io.event.ems.service;

import io.event.ems.dto.HoldResponseDTO;
import io.event.ems.dto.TicketHoldRequestDTO;
import io.event.ems.model.HoldData;

import java.util.UUID;

public interface TicketHoldService {

    HoldResponseDTO createAndValidateHold(UUID eventId, TicketHoldRequestDTO request, UUID userId);

    void releaseHold(UUID holdId, UUID userId);

    HoldData getAndFinalizeHold(UUID holdId, UUID userId);

    void releaseResourcesForFailedCheckout(HoldData holdData);
}
