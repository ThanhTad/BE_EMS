package io.event.ems.service.impl;

import io.event.ems.dto.EmailDetails;
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
    private static final String PURCHASE_CONFIRMATION_TEMPLATE_NAME = "purchase-confirmation";

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
    public void sendPurchaseConfirmationEmail(EmailDetails emailDetails) {
        final String toEmail = emailDetails.getToEmail();
        // Bạn có thể giữ nguyên subject hoặc làm cho nó chi tiết hơn
        final String subject = "Xác nhận đặt vé thành công - Sự kiện: " + emailDetails.getEventName();
        log.info("Attempting to send Purchase Confirmation email to {} for transaction {}", toEmail, emailDetails.getTransactionId());

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());

            // 1. CHUẨN BỊ CONTEXT CHO THYMELEAF (PHẦN CẦN THAY ĐỔI)
            Context context = new Context();

            // Các biến cơ bản
            context.setVariable("customerName", emailDetails.getCustomerName());
            context.setVariable("eventName", emailDetails.getEventName());
            context.setVariable("transactionId", emailDetails.getTransactionId());

            // CÁC BIẾN MỚI ĐƯỢC BỔ SUNG
            context.setVariable("eventTime", emailDetails.getEventTime());
            context.setVariable("venue", emailDetails.getVenue());
            context.setVariable("totalAmount", emailDetails.getTotalAmount());

            // BIẾN CHỨA DỮ LIỆU ĐÃ GOM NHÓM
            // Tên biến "ticketGroups" phải khớp với tên được dùng trong template (th:each="group : ${ticketGroups}")
            context.setVariable("ticketGroups", emailDetails.getTicketGroups());

            // 2. Render template HTML
            String htmlBody = templateEngine.process(PURCHASE_CONFIRMATION_TEMPLATE_NAME, context);

            // 3. Thiết lập thông tin người nhận và nội dung
            helper.setFrom(fromMail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            // 4. Nhúng các ảnh QR code vào email (Phần này không thay đổi)
            if (emailDetails.getInlineQrImages() != null && !emailDetails.getInlineQrImages().isEmpty()) {
                for (Map.Entry<String, byte[]> entry : emailDetails.getInlineQrImages().entrySet()) {
                    String cid = entry.getKey();
                    byte[] qrCodeImageBytes = entry.getValue();
                    helper.addInline(cid, new ByteArrayResource(qrCodeImageBytes), "image/png");
                }
            }

            // 5. Gửi email
            mailSender.send(mimeMessage);
            log.info("Successfully sent purchase confirmation email to {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send purchase confirmation email to {}. Error: {}", toEmail, e.getMessage());
            // Ném exception để hệ thống retry (nếu có cấu hình) có thể bắt được.
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
