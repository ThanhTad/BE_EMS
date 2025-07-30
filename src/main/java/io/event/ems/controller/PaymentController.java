package io.event.ems.controller;

import io.event.ems.dto.ApiResponse;
import io.event.ems.dto.PaymentCreationRequestDTO;
import io.event.ems.dto.PaymentCreationResultDTO;
import io.event.ems.dto.TicketPurchaseConfirmationDTO;
import io.event.ems.security.CustomUserDetails;
import io.event.ems.service.OrderProcessingService;
import io.event.ems.util.RequestUtils;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment Processing", description = "APIs for handling ticket payments")
public class PaymentController {

    private final OrderProcessingService orderProcessingService;

    /**
     * Endpoint để khởi tạo một yêu cầu thanh toán.
     * Dựa vào 'paymentMethod', service sẽ gọi đến cổng thanh toán tương ứng (MoMo, VNPAY, etc.).
     *
     * @param request            DTO chứa holdId và phương thức thanh toán.
     * @param currentUser        Thông tin người dùng đã xác thực.
     * @param httpServletRequest HttpServletRequest để lấy địa chỉ IP của client.
     * @return DTO chứa URL thanh toán để Frontend chuyển hướng người dùng.
     */
    @PostMapping("/create")
    @Operation(summary = "Create Payment Request", description = "Initiates a payment request with the selected payment gateway and returns a payment URL.")
    public ResponseEntity<ApiResponse<PaymentCreationResultDTO>> createPayment(
            @RequestBody PaymentCreationRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest httpServletRequest) {

        UUID userId = currentUser.getId();
        String ipAddress = RequestUtils.getClientIp(httpServletRequest);
        log.info("Creating payment for holdId: {}, method: {}, userId: {}, ip: {}",
                request.getHoldId(), request.getPaymentMethod(), userId, ipAddress);

        PaymentCreationResultDTO result = orderProcessingService.initiateRedirectPayment(
                request.getHoldId(),
                userId,
                request.getPaymentMethod(),
                ipAddress
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Endpoint để xác thực kết quả thanh toán trả về từ cổng thanh toán (qua Frontend).
     * Frontend sẽ gọi API này sau khi người dùng được chuyển hướng trở lại từ trang thanh toán.
     *
     * @param provider    Tên của cổng thanh toán (e.g., "momo", "vnpay").
     * @param params      Một Map chứa tất cả các query parameters từ URL trả về.
     * @param currentUser Thông tin người dùng đã xác thực.
     * @return DTO xác nhận đơn hàng đã được hoàn tất thành công.
     */
    @PostMapping("/verify/{provider}")
    @Operation(summary = "Verify Payment Result", description = "Verifies the payment result returned from the payment gateway via the client.")
    public ResponseEntity<ApiResponse<TicketPurchaseConfirmationDTO>> verifyPayment(
            @PathVariable String provider,
            @RequestBody Map<String, String> params,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        UUID userId = currentUser.getId();
        // Lấy orderId từ params, tương thích với cả VNPAY và MoMo
        String orderId = params.get("vnp_TxnRef"); // For VNPAY
        if (orderId == null) {
            orderId = params.get("orderId"); // For MoMo
        }

        if (orderId == null) {
            throw new IllegalArgumentException("Order ID not found in payment response parameters.");
        }
        log.info("Verifying payment for provider: {}, orderId: {}, userId: {}", provider, orderId, userId);

        TicketPurchaseConfirmationDTO result = orderProcessingService.verifyAndFinalizeRedirectedPurchase(
                provider.toUpperCase(),
                orderId,
                userId,
                params
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }


    /**
     * Endpoint MOCK để hoàn tất thanh toán mà không cần qua cổng thanh toán.
     * Endpoint này CHỈ TỒN TẠI ở các môi trường không phải production.
     * Được bảo vệ bởi Spring Profile.
     *
     * @param request     DTO chỉ chứa holdId.
     * @param currentUser Thông tin người dùng đã xác thực.
     * @return DTO xác nhận đơn hàng đã được hoàn tất thành công (giả lập).
     */
    @PostMapping("/mock-finalize")
    @Profile("!prod") // Kích hoạt cho mọi profile, TRỪ KHI profile là 'prod'
    @Hidden // Ẩn endpoint này khỏi tài liệu Swagger công khai
    @Operation(summary = "[DEV ONLY] Mock Finalize Purchase", description = "Finalizes a purchase without a real payment gateway. Only available in non-production environments.")
    public ResponseEntity<ApiResponse<TicketPurchaseConfirmationDTO>> mockFinalize(
            @RequestBody MockFinalizeRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        log.warn("======================================================================");
        log.warn("EXECUTING MOCK PAYMENT FINALIZATION. THIS SHOULD NOT BE IN PRODUCTION.");
        log.warn("======================================================================");

        UUID userId = currentUser.getId();
        TicketPurchaseConfirmationDTO result = orderProcessingService.mockFinalizePurchase(request.getHoldId(), userId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // DTO cho request của endpoint mock
    @Data
    public static class MockFinalizeRequestDTO {
        private UUID holdId;
    }
}
