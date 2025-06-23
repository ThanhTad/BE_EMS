package io.event.ems.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class BookingConfirmationDTO {

    private UUID purchaseId;
    private String eventName;
    private Instant purchaseDate;
    private BigDecimal totalPrice;
    private List<PurchasedItemDTO> purchasedItems;

}
