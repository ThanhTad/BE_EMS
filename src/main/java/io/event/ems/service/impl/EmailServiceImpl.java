package io.event.ems.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import io.event.ems.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("$spring.mail.username")
    private String fromMail;

    private static final String OTP_TEMPLATE_NAME = "email-otp";

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
            context.setVariable("recipientment", toMail);

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
    public void sendGroupTicketConfirmation(String toMail, String fullName, String transactionId,
            List<Map<String, Object>> tickets, Map<String, byte[]> inlineQrImages) {
        log.info("Attempting to send QR Code email to {}", toMail);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            Context context = new Context();
            context.setVariable("name", fullName);
            context.setVariable("transactionId", transactionId);
            context.setVariable("tickets", tickets);

            String html = templateEngine.process("group-ticket-confirmation", context);

            helper.setTo(toMail);
            helper.setSubject("Xác nhận đặt vé thành công - Giao dịch " + transactionId);
            helper.setText(html, true);

            if (inlineQrImages != null) {
                for (Map.Entry<String, byte[]> entry : inlineQrImages.entrySet()) {
                    String cid = entry.getKey();
                    byte[] qrCodeImages = entry.getValue();
                    helper.addInline(cid, new ByteArrayResource(qrCodeImages), "image/png");
                }
            }

            mailSender.send(message);
            log.info("Successfully sent group ticket confirmation to {}", toMail);
        } catch (MessagingException e) {
            throw new RuntimeException("Không thể gửi email với mã QR", e);
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
