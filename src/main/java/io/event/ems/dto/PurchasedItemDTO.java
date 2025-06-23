package io.event.ems.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PurchasedItemDTO {

    private String ticketName;      // Tên loại vé (Vd: "Vé VIP Khán đài A", "Vé Người Lớn")
    private BigDecimal price;       // Giá của một vé trong mục này

    // Dành cho vé tự do (General Admission)
    private Integer quantity;       // Số lượng vé đã mua (sẽ là 1 cho vé có chỗ ngồi)

    // Dành cho vé có chỗ ngồi (Reserved Seating), sẽ là null nếu là vé GA
    private SeatInfo seatInfo;

    // Inner DTO để gom thông tin ghế
    @Data
    @Builder
    public static class SeatInfo {
        private String sectionName;
        private String rowLabel;
        private String seatNumber;
    }
}
