package io.event.ems.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TicketResponseDTO {

    private UUID id;
    private UUID eventId;
    private String name;
    private BigDecimal price;
    private String description;
    private Integer maxPerPurchase;

    // Trả về cả ID và tên của Section để UI hiển thị dễ dàng
    private UUID appliesToSectionId;
    private String sectionName;

    private Integer totalQuantity;
    private Integer availableQuantity;

    private LocalDateTime saleStartDate;
    private LocalDateTime saleEndDate;

    private String statusName; // Trả về tên status, ví dụ: "ON_SALE"
    private LocalDateTime updatedAt;
}
