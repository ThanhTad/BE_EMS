package io.event.ems.service;

import io.event.ems.dto.PaymentCreationResultDTO;
import io.event.ems.model.TicketPurchase;

import java.math.BigDecimal;
import java.util.Map;

public interface PaymentGatewayService {

    String processPayment(String paymentToken, BigDecimal amount);

    /**
     * Chọn gateway phù hợp và ủy thác việc tạo URL thanh toán.
     *
     * @param provider  Tên của nhà cung cấp dịch vụ thanh toán (e.g., "MOMO", "VNPAY").
     * @param purchase  Đơn hàng cần tạo thanh toán.
     * @param ipAddress Địa chỉ IP của khách hàng.
     * @return DTO chứa URL thanh toán.
     */
    PaymentCreationResultDTO createPayment(String provider, TicketPurchase purchase, String ipAddress);

    /**
     * Chọn gateway phù hợp và ủy thác việc xác thực kết quả thanh toán.
     *
     * @param provider Tên của nhà cung cấp dịch vụ thanh toán.
     * @param params   Map các tham số trả về từ cổng thanh toán.
     * @return true nếu giao dịch hợp lệ, ngược lại false.
     */
    boolean verifyPayment(String provider, Map<String, String> params);
}
