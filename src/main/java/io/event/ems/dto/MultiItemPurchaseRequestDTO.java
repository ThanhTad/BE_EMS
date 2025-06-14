package io.event.ems.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MultiItemPurchaseRequestDTO {

    @NotNull(message = "User ID cannot be null")
    private UUID userId;

    @NotNull(message = "Purchase must contain at least one item")
    @Valid
    private List<PurchaseItemDTO> items;

}
