package io.event.ems.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import io.event.ems.dto.QrCodeVerificationResultDTO;
import io.event.ems.dto.TicketPurchaseDTO;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.mapper.TicketPurchaseMapper;
import io.event.ems.model.StatusCode;
import io.event.ems.model.TicketPurchase;
import io.event.ems.repository.StatusCodeRepository;
import io.event.ems.repository.TicketPurchaseRepository;
import io.event.ems.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class QrCodeServiceImpl implements QrCodeService {

    private static final int QR_CODE_WIDTH = 300;
    private static final int QR_CODE_HEIGHT = 300;
    private static final String QR_CODE_IMAGE_FORMAT = "PNG";
    private static final String DELIMETER = "|";

    @Value("${app.security.qr-secret-key}")
    private String secretKey;

    private final TicketPurchaseRepository ticketPurchaseRepository;
    private final StatusCodeRepository statusCodeRepository;
    private final TicketPurchaseMapper ticketPurchaseMapper;

    @Override
    public byte[] genarateQrCodeForPurchase(UUID purchaseId) throws ResourceNotFoundException {
        log.info("Generating QR Code for purchase ID: {}", purchaseId);
        if (!ticketPurchaseRepository.existsById(purchaseId)) {
            log.error("TicketPurchase not found with id: {}", purchaseId);
            throw new ResourceNotFoundException("TicketPurchase not found with id: {}" + purchaseId);
        }

        String purchaseIdStr = purchaseId.toString();
        String signature = hmacSha256(purchaseIdStr, secretKey);
        String qrContent = purchaseIdStr + DELIMETER + signature;
        log.debug("QR Code content for purchase ID {}: {}", purchaseId, qrContent);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        try {

            BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, QR_CODE_WIDTH, QR_CODE_HEIGHT);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, QR_CODE_IMAGE_FORMAT, output);
            log.info("Successfully generated QR Code for purchase ID: {}", purchaseId);
            return output.toByteArray();
        } catch (WriterException | IOException e) {
            log.error("Could not generate QR Code for purchase ID: {}", purchaseId, e);
            throw new RuntimeException("Error generating QR code", e);
        }

    }

    private String hmacSha256(String data, String secret) {
        try {

            if (secret == null || secret.trim().isEmpty()) {
                log.warn("HMAC secret key is null or empty. Please configure 'app.security.qr-secret-key'.");
                throw new IllegalArgumentException(
                        "HMAC secret key is missing or empty. Check application configuration.");
            }

            if (secret.length() < 32) {
                log.warn(
                        "HMAC secret key is shorter than recommended ({} characters). Consider using a longer, random key.",
                        32);
            }

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

    @Override
    public QrCodeVerificationResultDTO verifyQrCode(String qrCodeData) {
        log.info("Verifying QR Code data: {}", qrCodeData);

        String[] parts = qrCodeData.split("\\" + DELIMETER);
        if (parts.length != 2) {
            log.warn("Invalid QR Code format. Expected 2 parts, got {}. Date: {}", parts.length, qrCodeData);
            return new QrCodeVerificationResultDTO("INVALID_FORMAT", "Invalid QR Code format (Structure)", null);
        }

        String purchaseIdStr = parts[0];
        String receivedSignature = parts[1];

        String expectedSignature = hmacSha256(purchaseIdStr, secretKey);

        if (!expectedSignature.equals(receivedSignature)) {
            log.warn("QR Code signature mismatch for purchase ID string: {}. Expected: {}, Received: {}", purchaseIdStr,
                    expectedSignature, receivedSignature);
            return new QrCodeVerificationResultDTO("INVALID_FORMAT",
                    "QR Code signature verification failed (Tampered or Invalid Key)", null);
        }

        UUID purchaseId;
        try {
            purchaseId = UUID.fromString(purchaseIdStr);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid purchase ID format in QR Code: {}", purchaseIdStr);
            return new QrCodeVerificationResultDTO("INVALID_FORMAT", "Invalid purchase ID format.", null);
        }

        Optional<TicketPurchase> purchaseOpt = ticketPurchaseRepository.findById(purchaseId);
        if (purchaseOpt.isEmpty()) {
            log.warn("TicketPurchase not found for verified QR Code ID: {}", purchaseId);
            return new QrCodeVerificationResultDTO("NOT_FOUND",
                    "Ticket purchase not found.", null);
        }

        TicketPurchase purchase = purchaseOpt.get();
        TicketPurchaseDTO dto = ticketPurchaseMapper.toDTO(purchase);

        StatusCode checkedInStatus = statusCodeRepository.findByEntityTypeAndStatus("TICKET_PURCHASE", "CHECKED_IN")
                .orElseThrow(
                        () -> new IllegalStateException("Status 'CHECKED_IN' for TICKET_PURCHASE not configured."));
        StatusCode successStatus = statusCodeRepository.findByEntityTypeAndStatus("TICKET_PURCHASE", "SUCCESS")
                .orElseThrow(() -> new IllegalStateException("Status 'SUCCESS' for TICKET_PURCHASE not configured."));

        if (purchase.getStatus().getId().equals(checkedInStatus.getId())) {
            log.info("QR Code ID {} already checked in.", purchaseId);
            return new QrCodeVerificationResultDTO("ALREADY_CHECKED_IN", "Ticket already checked in.", dto);
        }

        if (!purchase.getStatus().getId().equals(successStatus.getId())) {
            log.warn("QR Code ID {} has an invalid purchase status ({}) for check-in.", purchaseId,
                    purchase.getStatus().getStatus());
            return new QrCodeVerificationResultDTO("PURCHASE_INVALID",
                    "Ticket purchase status is not valid for check-in.", dto);
        }

        purchase.setStatus(checkedInStatus);
        ticketPurchaseRepository.save(purchase);
        dto.setStatusId(checkedInStatus.getId());
        log.info("QR Code ID {} successfully verified and checked in.", purchaseId);

        return new QrCodeVerificationResultDTO("SUCCESS", "Verification successful. Checked in.", dto);
    }

}
