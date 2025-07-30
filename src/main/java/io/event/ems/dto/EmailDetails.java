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
    private String transactionId;

    private String eventTime;
    private String venue;
    private BigDecimal totalAmount;

    // --- Danh sách các vé đã mua để hiển thị trong email ---
    private List<TicketGroupInfo> ticketGroups;

    // --- Dữ liệu ảnh QR Code để nhúng vào email ---
    // Key: Content-ID (CID) để tham chiếu trong template HTML.
    // Value: Dữ liệu byte của ảnh QR.
    private Map<String, byte[]> inlineQrImages;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketGroupInfo {
        private String ticketName;
        private List<TicketInfo> tickets; // Danh sách các vé cá nhân trong nhóm này
    }

    /**
     * Lớp con chứa thông tin chi tiết của MỘT vé đơn lẻ.
     * Bỏ các trường đã chuyển lên cấp nhóm.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketInfo {
        private BigDecimal pricePerItem;
        private String details; // e.g., "Khu A - Hàng B - Ghế 12" hoặc "Vé vào cửa tự do"
        private String qrCodeCid; // Content-ID để liên kết với ảnh QR trong Map
    }
}
