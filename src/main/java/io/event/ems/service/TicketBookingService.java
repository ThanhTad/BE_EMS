package io.event.ems.service;

import io.event.ems.dto.BookingConfirmationDTO;
import io.event.ems.dto.CheckoutRequestDTO;
import io.event.ems.dto.HoldRequestDTO;
import io.event.ems.dto.HoldResponseDTO;

public interface TicketBookingService {

    HoldResponseDTO hold(HoldRequestDTO request);

    BookingConfirmationDTO checkout(CheckoutRequestDTO request);
}
