package io.event.ems.service.specialized;

import io.event.ems.dto.HoldRequestDTO;
import io.event.ems.dto.HoldResponseDTO;

public interface SeatBookingService {

    HoldResponseDTO holdSeats(HoldRequestDTO request);
}
