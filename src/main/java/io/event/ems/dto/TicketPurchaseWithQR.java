package io.event.ems.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketPurchaseWithQR {

    private UUID purchaseId;
    private String eventName;
    private LocalDateTime eventDate;
    private String seatNumber;
    private String ticketType;
    private String qrCodeCid;

}
