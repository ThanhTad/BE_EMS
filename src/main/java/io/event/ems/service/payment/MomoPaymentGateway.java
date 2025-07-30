package io.event.ems.service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import io.event.ems.dto.PaymentCreationResultDTO;
import io.event.ems.model.TicketPurchase;
import io.event.ems.util.MomoSecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
public class MomoPaymentGateway implements PaymentGateway {

    @Value("${payment.momo.partner-code}")
    private String partnerCode;
    @Value("${payment.momo.access-key}")
    private String accessKey;
    @Value("${payment.momo.secret-key}")
    private String secretKey;
    @Value("${payment.momo.api-endpoint}")
    private String apiEndpoint;
    @Value("${payment.momo.return-url}")
    private String returnUrl;
    @Value("${payment.momo.notify-url}")
    private String notifyUrl;

    @Override
    public String getProviderName() {
        return "MOMO";
    }

    @Override
    public PaymentCreationResultDTO createPaymentUrl(TicketPurchase purchase, String ipAddress) {
        String orderId = purchase.getId().toString();
        String requestId = UUID.randomUUID().toString();
        long amount = purchase.getTotalPrice().longValue();
        String orderInfo = "Thanh toan don hang #" + purchase.getId().toString().substring(0, 8);

        // 1. TẠO CHUỖI RAW ĐỂ TẠO CHỮ KÝ
        // QUAN TRỌNG: Thứ tự các tham số phải chính xác theo tài liệu của MoMo
        String rawSignature = String.format("accessKey=%s&amount=%d&extraData=&ipnUrl=%s&orderId=%s&orderInfo=%s&partnerCode=%s&redirectUrl=%s&requestId=%s&requestType=captureWallet",
                accessKey, amount, notifyUrl, orderId, orderInfo, partnerCode, returnUrl, requestId);

        log.info("MoMo Raw Signature String: {}", rawSignature);

        // 2. TẠO CHỮ KÝ HMAC_SHA256
        String signature = MomoSecurityUtils.generateSignature(rawSignature, secretKey);
        log.info("MoMo Signature: {}", signature);

        // 3. TẠO REQUEST BODY DẠNG JSON
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("partnerCode", partnerCode);
        requestBody.put("requestId", requestId);
        requestBody.put("amount", amount);
        requestBody.put("orderId", orderId);
        requestBody.put("orderInfo", orderInfo);
        requestBody.put("redirectUrl", returnUrl);
        requestBody.put("ipnUrl", notifyUrl);
        requestBody.put("requestType", "captureWallet");
        requestBody.put("lang", "vi");
        requestBody.put("signature", signature);

        // 4. GỌI API CỦA MOMO
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(apiEndpoint, entity, JsonNode.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode responseBody = response.getBody();
                int resultCode = responseBody.get("resultCode").asInt();
                if (resultCode == 0) {
                    String payUrl = responseBody.get("payUrl").asText();
                    log.info("Successfully created MoMo payment URL for order [ID= {}]", orderId);
                    return new PaymentCreationResultDTO(payUrl);
                } else {
                    String message = responseBody.get("message").asText();
                    log.error("Failed to create MoMo payment URL. ResultCode: {}, Message: {}", resultCode, message);
                    throw new RuntimeException("MoMo Error: " + message);
                }
            } else {
                log.error("Failed to communicate with MoMo API. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("Failed to create MoMo payment URL due to API communication error.");
            }
        } catch (Exception e) {
            log.error("Exception occurred while creating MoMo payment for order [ID={}]", orderId, e);
            throw new RuntimeException("System error while creating MoMo payment.");
        }
    }

    /**
     * Xử lý và xác thực các tham số MoMo trả về trên returnUrl.
     *
     * @param params Map chứa tất cả các query parameter từ MoMo.
     * @return true nếu giao dịch hợp lệ và thành công, ngược lại false.
     */
    @Override
    public boolean handlePaymentReturn(Map<String, String> params) {
        // Lấy chữ ký từ MoMo và xóa nó khỏi map để chuẩn bị tạo lại chữ ký
        String momoSignature = params.get("signature");
        if (momoSignature == null || momoSignature.isBlank()) {
            log.warn("MoMo payment return is missing signature.");
            return false;
        }
        params.remove("signature");

        // 1. TẠO CHUỖI RAW ĐỂ XÁC THỰC CHỮ KÝ
        // QUAN TRỌNG: Thứ tự các tham số phải được sắp xếp theo Alphabet
        String rawSignature = params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));

        log.debug("MoMo Return Raw Signature String: {}", rawSignature);

        // 2. TẠO LẠI CHỮ KÝ TỪ DỮ LIỆU
        String expectedSignature = MomoSecurityUtils.generateSignature(rawSignature, secretKey);
        log.debug("MoMo Expected Signature: {}, Received Signature: {}", expectedSignature, momoSignature);

        // 3. SO SÁNH CHỮ KÝ VÀ KIỂM TRA RESULTCODE
        boolean isSignatureValid = expectedSignature.equals(momoSignature);
        boolean isPaymentSuccessful = "0".equals(params.get("resultCode"));

        if (!isSignatureValid) {
            log.error("CRITICAL: MoMo signature mismatch for order [ID={}]. Payment cannot be trusted.", params.get("orderId"));
        }
        if (!isPaymentSuccessful) {
            log.warn("MoMo payment was not successful for order [ID={}]. ResultCode: {}, Message: {}",
                    params.get("orderId"), params.get("resultCode"), params.get("message"));
        }

        return isSignatureValid && isPaymentSuccessful;
    }
}
