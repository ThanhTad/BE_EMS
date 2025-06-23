package io.event.ems.controller;

import io.event.ems.dto.*;
import io.event.ems.service.TicketBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final TicketBookingService ticketBookingService;

    @PostMapping("/hold")
    public ResponseEntity<ApiResponse<?>> hold(@RequestBody HoldRequestDTO holdRequest) {
        HoldResponseDTO response = ticketBookingService.hold(holdRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<?>> checkout(@RequestBody CheckoutRequestDTO checkoutRequest) {
        BookingConfirmationDTO response = ticketBookingService.checkout(checkoutRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
