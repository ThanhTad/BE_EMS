package io.event.ems.service;

import io.event.ems.dto.PaymentCreationResultDTO;
import io.event.ems.dto.PaymentDetailsDTO;
import io.event.ems.dto.TicketPurchaseConfirmationDTO;

import java.util.Map;
import java.util.UUID;

public interface OrderProcessingService {


    /**
     * Hoàn tất một đơn hàng sử dụng luồng thanh toán trực tiếp, nơi thông tin thanh toán
     * (ví dụ: payment token) được xử lý ngay lập tức ở backend.
     * Áp dụng cho các cổng như Stripe.
     *
     * @param holdId         ID của phiên giữ chỗ đã được xác thực.
     * @param userId         ID của người dùng thực hiện giao dịch.
     * @param paymentDetails DTO chứa thông tin chi tiết về thanh toán (payment token).
     * @return DTO chứa thông tin xác nhận đơn hàng sau khi hoàn tất.
     */
    TicketPurchaseConfirmationDTO finalizeDirectPurchase(UUID holdId, UUID userId, PaymentDetailsDTO paymentDetails);

    /**
     * Bắt đầu một quy trình thanh toán theo luồng chuyển hướng.
     * Phương thức này sẽ tạo một đơn hàng với trạng thái PENDING và gọi đến cổng thanh toán
     * để lấy URL chuyển hướng cho người dùng.
     * Áp dụng cho các cổng như MoMo, VNPAY.
     *
     * @param holdId        ID của phiên giữ chỗ đã được xác thực.
     * @param userId        ID của người dùng thực hiện giao dịch.
     * @param paymentMethod Tên của nhà cung cấp dịch vụ thanh toán (e.g., "MOMO", "VNPAY").
     * @param ipAddress     Địa chỉ IP của người dùng, cần thiết cho một số cổng thanh toán.
     * @return DTO chứa URL thanh toán để Frontend chuyển hướng người dùng.
     */
    PaymentCreationResultDTO initiateRedirectPayment(UUID holdId, UUID userId, String paymentMethod, String ipAddress);

    /**
     * Xác thực và hoàn tất một đơn hàng sau khi người dùng được cổng thanh toán
     * chuyển hướng trở lại hệ thống.
     * Phương thức này sẽ kiểm tra chữ ký điện tử và cập nhật trạng thái đơn hàng từ PENDING sang SUCCESS.
     *
     * @param provider Tên của nhà cung cấp dịch vụ thanh toán đã xử lý giao dịch.
     * @param orderId  ID của đơn hàng (do hệ thống của ta tạo ra, thường được gửi đi và nhận về từ cổng thanh toán).
     * @param userId   ID của người dùng để xác thực quyền sở hữu đơn hàng.
     * @param params   Một Map chứa tất cả các tham số query mà cổng thanh toán trả về trên URL.
     * @return DTO chứa thông tin xác nhận đơn hàng sau khi hoàn tất.
     */
    TicketPurchaseConfirmationDTO verifyAndFinalizeRedirectedPurchase(String provider, String orderId, UUID userId, Map<String, String> params);

    /**
     * Hoàn tất một đơn hàng giả lập cho mục đích kiểm thử (testing).
     * Phương thức này bỏ qua hoàn toàn bước gọi đến cổng thanh toán và trực tiếp
     * cập nhật trạng thái đơn hàng thành SUCCESS.
     * Chỉ nên được kích hoạt ở môi trường development/testing.
     *
     * @param holdId ID của phiên giữ chỗ đã được xác thực.
     * @param userId ID của người dùng thực hiện giao dịch.
     * @return DTO chứa thông tin xác nhận đơn hàng giả lập.
     */
    TicketPurchaseConfirmationDTO mockFinalizePurchase(UUID holdId, UUID userId);
}
