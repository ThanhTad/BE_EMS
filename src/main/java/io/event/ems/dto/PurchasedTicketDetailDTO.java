package io.event.ems.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class PurchasedTicketDetailDTO {

    private UUID ticketId;
    private String ticketName;
    private BigDecimal price;

    //Dành cho vé có chỗ ngồi
    private UUID seatId;
    private String seatName;
    private String rowLabel;
    private String seatNumber;

    //Dành cho vé tự do
    private Integer quantity;
}
