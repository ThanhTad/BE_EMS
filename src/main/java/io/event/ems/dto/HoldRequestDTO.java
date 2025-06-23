package io.event.ems.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class HoldRequestDTO {

    private UUID eventId;
    private UUID userId;

    // Cho RESERVED/ZONED
    private List<UUID> seatIds;

    // Cho GENERAL ADMISSION - Giờ là một danh sách
    private List<GeneralAdmissionHoldItem> gaItems;

    @Data
    public static class GeneralAdmissionHoldItem {
        private UUID ticketId;
        private Integer quantity;
    }
}


