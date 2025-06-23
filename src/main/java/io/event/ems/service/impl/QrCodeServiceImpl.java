package io.event.ems.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import io.event.ems.model.EventSeatStatus;
import io.event.ems.model.PurchasedGaTicket;
import io.event.ems.model.TicketQrCode;
import io.event.ems.repository.TicketQrCodeRepository;
import io.event.ems.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QrCodeServiceImpl implements QrCodeService {

    private static final int QR_CODE_WIDTH = 300;
    private static final int QR_CODE_HEIGHT = 300;
    private static final String QR_CODE_IMAGE_FORMAT = "PNG";
    private static final String DELIMITER = "|";

    @Value("${app.security.qr-secret-key}")
    private String secretKey;

    private final TicketQrCodeRepository ticketQrCodeRepository;

    @Override
    public byte[] generateQrCodeForReservedSeat(EventSeatStatus soldSeat) {
        // Tạo một bản ghi QR Code trong DB trước để có ID
        TicketQrCode qrEntity = new TicketQrCode();
        qrEntity.setEventSeat(soldSeat);
        TicketQrCode savedQrEntity = ticketQrCodeRepository.save(qrEntity);

        return generateQrCodeImage(savedQrEntity.getId());
    }

    @Override
    public byte[] generateQrCodeForGaTicketGroup(PurchasedGaTicket gaTicketGroup) {
        // Logic tương tự cho vé GA
        TicketQrCode qrEntity = new TicketQrCode();
        qrEntity.setPurchasedGaTicket(gaTicketGroup); // Giả sử có liên kết này
        TicketQrCode savedQrEntity = ticketQrCodeRepository.save(qrEntity);

        return generateQrCodeImage(savedQrEntity.getId());
    }

    /**
     * Hàm chung để tạo ảnh QR từ một ID duy nhất (ID của bản ghi TicketQrCode)
     */
    private byte[] generateQrCodeImage(UUID uniqueId) {
        String idStr = uniqueId.toString();
        String signature = hmacSha256(idStr, secretKey);
        String qrContent = idStr + DELIMITER + signature;
        log.debug("Generating QR Code with content: {}", qrContent);

        try {
            BitMatrix bitMatrix = new QRCodeWriter().encode(qrContent, BarcodeFormat.QR_CODE, QR_CODE_WIDTH, QR_CODE_HEIGHT);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, QR_CODE_IMAGE_FORMAT, output);
            log.info("Successfully generated QR Code for ID: {}", uniqueId);
            return output.toByteArray();
        } catch (WriterException | IOException e) {
            log.error("Could not generate QR Code for ID: {}", uniqueId, e);
            throw new RuntimeException("Error generating QR code", e);
        }
    }

    private String hmacSha256(String data, String secret) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(keySpec);
            byte[] result = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(result);
        } catch (Exception e) {
            log.error("Failed to calculate HMAC-SHA256", e);
            throw new RuntimeException("Failed to calculate HMAC-SHA256", e);
        }
    }

}
