package io.event.ems.service.specialized;

import io.event.ems.dto.HoldRequestDTO;
import io.event.ems.dto.HoldResponseDTO;

public interface GeneralAdmissionBookingService {

    HoldResponseDTO holdTickets(HoldRequestDTO request);
}
