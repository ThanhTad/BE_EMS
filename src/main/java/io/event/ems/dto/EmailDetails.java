package io.event.ems.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailDetails {

    // --- Thông tin người nhận và đơn hàng ---
    private String toEmail;
    private String customerName;
    private String eventName;
    private String transactionId; // Hoặc purchaseId

    // --- Danh sách các vé đã mua để hiển thị trong email ---
    private List<TicketInfo> purchasedTickets;

    // --- Dữ liệu ảnh QR Code để nhúng vào email ---
    // Key: Content-ID (CID) để tham chiếu trong template HTML.
    // Value: Dữ liệu byte của ảnh QR.
    private Map<String, byte[]> inlineQrImages;

    /**
     * Lớp con chứa thông tin chi tiết của một mục vé đã mua.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketInfo {
        private String ticketName;
        private int quantity;
        private BigDecimal pricePerItem;
        private String details; // e.g., "Khu A - Hàng B - Ghế 12" hoặc "Vé vào cửa"
        private String qrCodeCid; // Content-ID để liên kết với ảnh QR trong Map
    }
}
