package io.event.ems.service.impl;

import io.event.ems.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("$spring.mail.username")
    private String fromMail;

    private static final String OTP_TEMPLATE_NAME = "email-otp";
    private static final String GROUP_TICKET_TEMPLATE_NAME = "group-ticket-confirmation";

    @Override
    @Async
    public void sendOtpEmail(String toMail, String subject, String otp) {

        log.info("Attempting to send OTP email to {} with subject '{}'", toMail, subject);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());

            Context context = new Context();
            context.setVariable("subject", subject);
            context.setVariable("otpCode", otp);
            context.setVariable("recipient", toMail);

            String htmlBody = templateEngine.process(OTP_TEMPLATE_NAME, context);

            helper.setTo(toMail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            helper.setFrom(fromMail);

            mailSender.send(message);
            log.info("OTP email sent successfully to {}", toMail);

        } catch (MessagingException e) {
            log.error("Failed to send email to {} with subject '{}'", toMail, subject, e);
        } catch (Exception e) {
            log.error("An unexpected error occurred while sending email to {}", toMail, e);
        }

    }

    @Override
    @Async
    public void sendGroupTicketConfirmation(String toEmail, String fullName, String transactionId, String eventName, List<Map<String, Object>> ticketDetails, Map<String, byte[]> inlineQrImages) {
        log.info("Attempting to send Group Ticket Confirmation email to {} for transaction {}", toEmail, transactionId);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            // Tham số 'true' bật chế độ multipart, cần thiết cho việc đính kèm và nhúng ảnh
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());

            // =========================================================
            // === BƯỚC 1: CHUẨN BỊ DỮ LIỆU CHO TEMPLATE (CONTEXT) ===
            // =========================================================
            Context context = new Context();
            context.setVariable("name", fullName);
            context.setVariable("transactionId", transactionId);
            context.setVariable("eventName", eventName);
            context.setVariable("tickets", ticketDetails); // "tickets" là tên biến mà th:each trong template sẽ sử dụng

            // =========================================================
            // === BƯỚC 2: RENDER TEMPLATE HTML VỚI DỮ LIỆU ĐÃ CÓ ===
            // =========================================================
            String htmlBody = templateEngine.process(GROUP_TICKET_TEMPLATE_NAME, context);

            // =========================================================
            // === BƯỚC 3: THIẾT LẬP THÔNG TIN EMAIL VÀ NHÚNG ẢNH QR ===
            // =========================================================
            helper.setTo(toEmail);
            helper.setSubject("Xác nhận đặt vé thành công - Sự kiện: " + eventName);
            helper.setText(htmlBody, true); // Tham số 'true' chỉ định nội dung là HTML
            helper.setFrom(fromMail);

            // Nhúng các ảnh QR code vào email
            if (inlineQrImages != null && !inlineQrImages.isEmpty()) {
                for (Map.Entry<String, byte[]> entry : inlineQrImages.entrySet()) {
                    String cid = entry.getKey();
                    byte[] qrCodeImageBytes = entry.getValue();
                    // Thêm ảnh vào email với Content-ID (CID) tương ứng
                    helper.addInline(cid, new ByteArrayResource(qrCodeImageBytes), "image/png");
                }
            }

            // =========================================================
            // === BƯỚC 4: GỬI EMAIL ===
            // =========================================================
            mailSender.send(mimeMessage);
            log.info("Successfully sent group ticket confirmation to {} for transaction {}", toEmail, transactionId);

        } catch (MessagingException e) {
            log.error("Failed to send email with QR codes to {}: {}", toEmail, e.getMessage());
            // Trong môi trường production, bạn có thể lưu lại email này vào một hàng đợi để thử gửi lại
            throw new RuntimeException("Failed to send confirmation email", e);
        }
    }

    @Async
    @Override
    public void sendWelcomeEmail(String to, String username) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            Context context = new Context();
            context.setVariable("username", username);

            String htmlContent = templateEngine.process("welcome-email", context);

            helper.setTo(to);
            helper.setSubject("Welcome to EMS!");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send welcome email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send welcome email", e);
        }
    }

}
