package io.event.ems.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.event.ems.model.TicketSelectionModeEnum;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
public class TicketHoldRequestDTO {

    private TicketSelectionModeEnum selectionMode;
    private int holdDurationMinutes = 10;

    // For RESERVED_SEATING
    private List<UUID> seatIds;

    // For GENERAL_ADMISSION / ZONED_ADMISSION
    private List<GeneralAdmissionItem> gaItems;

    @Data
    @NoArgsConstructor
    public static class GeneralAdmissionItem {
        private UUID ticketId;
        private int quantity;
    }

    @JsonIgnore
    public boolean isValidForMode() {
        return switch (selectionMode) {
            case GENERAL_ADMISSION, ZONED_ADMISSION ->
                    gaItems != null && !gaItems.isEmpty() && (seatIds == null || seatIds.isEmpty());
            case RESERVED_SEATING -> seatIds != null && !seatIds.isEmpty() && (gaItems == null || gaItems.isEmpty());
        };
    }
}
