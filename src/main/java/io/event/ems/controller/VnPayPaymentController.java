package io.event.ems.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.event.ems.service.TicketPurchaseService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RestController("/api/v1/payments/vnpay")
@RequiredArgsConstructor
@Slf4j
public class VnPayPaymentController {

    private final TicketPurchaseService service;

    @GetMapping("/ipn")
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleVnPayIpn(HttpServletRequest request) {
        log.info("Received VNPay IPN request");
        Map<String, String> response = service.processVnPayIpn(request);
        log.info("Responding to VNPay IPN: {}", response);
        return ResponseEntity.ok(response);

    }

    @GetMapping("/return")
    public String handleVnPayReturn(HttpServletRequest request, Model model) {
        log.info("Received VNPay return request");
        boolean isValid = service.verifyVnPayReturn(request);
        String vnp_ResponseCode = request.getParameter("vnp_ResponseCode");
        String vnp_TxnRef = request.getParameter("vnp_TxnRef");
        String message;
        String viewName;

        if (isValid) {
            if ("00".equals(vnp_ResponseCode)) {
                log.info("VNPay return success for TxnRef: {}", vnp_TxnRef);
                message = "Giao dịch thanh toán thành công! Vui lòng kiểm tra email để nhận vé.";
                viewName = "payment_success";
            } else {
                log.warn("VNPay return failed for TxnRef: {} with code: {}", vnp_TxnRef, vnp_ResponseCode);
                message = "Giao dịch thanh toán thất bại. Mã lỗi VNPay: " + vnp_ResponseCode
                        + ". Vui lòng thử lại hoặc liên hệ hỗ trợ.";
                viewName = "payment_failed";
            }
        } else {
            log.error("VNPay return verification failed for TxnRef: {}", vnp_TxnRef);
            message = "Xác thực giao dịch thất bại. Vui lòng liên hệ hỗ trợ.";
            viewName = "payment_failed";
        }
        model.addAttribute("message", message);
        model.addAttribute("orderId", vnp_TxnRef);
        return viewName;

    }

}
