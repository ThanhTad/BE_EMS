package io.event.ems.service.payment;

import io.event.ems.dto.PaymentCreationResultDTO;
import io.event.ems.model.TicketPurchase;
import io.event.ems.util.VNPaySecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

@Component
@Slf4j
public class VNPayPaymentGateway implements PaymentGateway {

    @Value("${payment.vnpay.tmn-code}")
    private String tmnCode;
    @Value("${payment.vnpay.hash-secret}")
    private String hashSecret;
    @Value("${payment.vnpay.api-url}")
    private String apiUrl;
    @Value("${payment.vnpay.return-url}")
    private String returnUrl;


    @Override
    public String getProviderName() {
        return "VNPay";
    }

    @Override
    public PaymentCreationResultDTO createPaymentUrl(TicketPurchase purchase, String ipAddress) {
        long amount = purchase.getTotalPrice().multiply(new BigDecimal(100)).longValue();
        String orderInfo = "Thanh toan don hang #" + purchase.getId().toString().substring(0, 8);

        // 1. TẠO MAP CHỨA CÁC THAM SỐ GỬI ĐẾN VNPAY
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", tmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", purchase.getId().toString()); // Mã tham chiếu của giao dịch tại hệ thống của bạn
        vnp_Params.put("vnp_OrderInfo", orderInfo);
        vnp_Params.put("vnp_OrderType", "other"); // Mã loại hàng hoá
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", returnUrl);
        vnp_Params.put("vnp_IpAddr", ipAddress);

        // Thời gian tạo giao dịch
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));

        // Thời gian hết hạn
        cld.add(Calendar.MINUTE, 15);
        vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        // 2. TẠO QUERY STRING TỪ MAP
        // Các tham số phải được sắp xếp theo alphabet
        String queryString = VNPaySecurityUtils.buildQueryString(vnp_Params);
        log.debug("VNPAY Raw Query String: {}", queryString);

        // 3. TẠO CHỮ KÝ HMAC_SHA512
        String vnp_SecureHash = VNPaySecurityUtils.generateSignature(queryString, hashSecret);
        log.debug("VNPAY Signature: {}", vnp_SecureHash);

        // 4. TẠO URL THANH TOÁN HOÀN CHỈNH
        String paymentUrl = apiUrl + "?" + queryString + "&vnp_SecureHash=" + vnp_SecureHash;
        log.info("Successfully created VNPAY payment URL for order [ID={}]", purchase.getId());

        return new PaymentCreationResultDTO(paymentUrl);
    }

    @Override
    public boolean handlePaymentReturn(Map<String, String> params) {
        // Lấy chữ ký từ VNPAY và xóa nó khỏi map để chuẩn bị tạo lại chữ ký
        String vnp_SecureHash = params.get("vnp_SecureHash");
        if (vnp_SecureHash == null || vnp_SecureHash.isBlank()) {
            log.warn("VNPAY payment return is missing signature.");
            return false;
        }
        params.remove("vnp_SecureHash");
        params.remove("vnp_SecureHashType"); // Loại bỏ nếu có

        // 1. TẠO LẠI QUERY STRING TỪ CÁC THAM SỐ CÒN LẠI
        String queryString = VNPaySecurityUtils.buildQueryString(params);
        log.debug("VNPAY Return Raw Query String: {}", queryString);

        // 2. TẠO LẠI CHỮ KÝ TỪ DỮ LIỆU
        String expectedSignature = VNPaySecurityUtils.generateSignature(queryString, hashSecret);
        log.debug("VNPAY Expected Signature: {}, Received Signature: {}", expectedSignature, vnp_SecureHash);

        // 3. SO SÁNH CHỮ KÝ VÀ KIỂM TRA MÃ PHẢN HỒI (vnp_ResponseCode)
        boolean isSignatureValid = expectedSignature.equalsIgnoreCase(vnp_SecureHash); // VNPAY có thể trả về chữ hoa
        boolean isPaymentSuccessful = "00".equals(params.get("vnp_ResponseCode"));

        if (!isSignatureValid) {
            log.error("CRITICAL: VNPAY signature mismatch for order [ID={}]. Payment cannot be trusted.", params.get("vnp_TxnRef"));
        }
        if (!isPaymentSuccessful) {
            log.warn("VNPAY payment was not successful for order [ID={}]. ResponseCode: {}",
                    params.get("vnp_TxnRef"), params.get("vnp_ResponseCode"));
        }

        return isSignatureValid && isPaymentSuccessful;
    }
}
