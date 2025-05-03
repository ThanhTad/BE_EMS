package io.event.ems.dto;

public record QrCodeVerificationResultDTO(
                String status,
                String message,
                TicketPurchaseDTO ticketPurchaseDTO) {

}
