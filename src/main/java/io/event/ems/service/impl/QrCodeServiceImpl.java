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
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QrCodeServiceImpl implements QrCodeService {

    private static final int QR_CODE_WIDTH = 300;
    private static final int QR_CODE_HEIGHT = 300;
    private static final String QR_CODE_IMAGE_FORMAT = "PNG";
    private static final String QR_CONTENT_DELIMITER = "|";

    @Value("${app.security.qr-secret-key}")
    private String secretKey;

    private final TicketQrCodeRepository ticketQrCodeRepository;

    @Override
    @Transactional
    public byte[] generateQrCodeForReservedSeat(EventSeatStatus soldSeat) {
        log.info("Generating QR Code for reserved seat [ID={}]", soldSeat.getId());

        // 1. Tạo một UUID mới để làm định danh duy nhất cho mã QR này.
        UUID uniqueIdentifier = UUID.randomUUID();

        // 2. Tạo nội dung và ảnh QR trước.
        String qrContent = buildSecureQrContent(uniqueIdentifier);
        byte[] qrImage = generateQrImage(qrContent);

        // 3. Nếu tạo ảnh thành công, tiến hành lưu bản ghi vào DB.
        TicketQrCode qrEntity = new TicketQrCode();
        qrEntity.setId(uniqueIdentifier); // Sử dụng UUID đã tạo làm Primary Key
        qrEntity.setEventSeat(soldSeat);
        qrEntity.setQrCodeData(qrContent); // Lưu lại nội dung đã mã hóa
        qrEntity.setGeneratedAt(Instant.from(LocalDateTime.now()));

        ticketQrCodeRepository.save(qrEntity);

        log.info("Successfully created and saved QR Code [ID={}] for reserved seat.", uniqueIdentifier);
        return qrImage;
    }

    /**
     * Tạo mã QR cho một nhóm vé GA (ví dụ: mua 3 vé GA cùng loại).
     * Sẽ chỉ có MỘT mã QR cho cả nhóm vé này.
     */
    @Override
    @Transactional
    public byte[] generateQrCodeForGaTicket(PurchasedGaTicket gaTicketGroup) {
        log.info("Generating QR Code for GA ticket group [ID={}]", gaTicketGroup.getId());

        // Logic tương tự như vé ghế
        UUID uniqueIdentifier = UUID.randomUUID();
        String qrContent = buildSecureQrContent(uniqueIdentifier);
        byte[] qrImage = generateQrImage(qrContent);

        TicketQrCode qrEntity = new TicketQrCode();
        qrEntity.setId(uniqueIdentifier);
        qrEntity.setPurchasedGaTicket(gaTicketGroup);
        qrEntity.setQrCodeData(qrContent);
        qrEntity.setGeneratedAt(Instant.from(LocalDateTime.now()));

        ticketQrCodeRepository.save(qrEntity);

        log.info("Successfully created and saved QR Code [ID={}] for GA ticket group.", uniqueIdentifier);
        return qrImage;
    }

    // =================================================================
    // === PRIVATE HELPER METHODS ===
    // =================================================================

    /**
     * Xây dựng nội dung an toàn cho QR code.
     * Nội dung bao gồm ID và một chữ ký số HMAC-SHA256.
     *
     * @param uniqueId ID duy nhất của bản ghi TicketQrCode.
     * @return Chuỗi nội dung để mã hóa thành ảnh QR.
     */
    private String buildSecureQrContent(UUID uniqueId) {
        String idStr = uniqueId.toString();
        String signature = hmacSha256(idStr, secretKey);
        return idStr + QR_CONTENT_DELIMITER + signature;
    }

    /**
     * Hàm chung để tạo ảnh QR từ một chuỗi nội dung.
     *
     * @param qrContent Nội dung cần mã hóa.
     * @return Dữ liệu byte của ảnh QR theo định dạng PNG.
     */
    private byte[] generateQrImage(String qrContent) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, QR_CODE_WIDTH, QR_CODE_HEIGHT);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, QR_CODE_IMAGE_FORMAT, pngOutputStream);
            return pngOutputStream.toByteArray();

        } catch (WriterException | IOException e) {
            log.error("Could not generate QR Code image for content: {}", qrContent, e);
            // Ném ra một exception cụ thể hơn hoặc RuntimeException để transaction có thể rollback
            throw new RuntimeException("Error occurred during QR code image generation.", e);
        }
    }

    /**
     * Tính toán chữ ký HMAC-SHA256 cho một chuỗi dữ liệu.
     *
     * @param data   Dữ liệu cần ký.
     * @param secret Khóa bí mật.
     * @return Chuỗi chữ ký đã được mã hóa Base64 URL-safe.
     */
    private String hmacSha256(String data, String secret) {
        try {
            byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretBytes, "HmacSHA256");

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKeySpec);

            byte[] hmacSha256Bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // Sử dụng Base64 URL-safe để tránh các ký tự đặc biệt có thể gây lỗi trong URL
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacSha256Bytes);
        } catch (Exception e) {
            log.error("Failed to calculate HMAC-SHA256 signature", e);
            throw new RuntimeException("Failed to calculate signature", e);
        }
    }

}
