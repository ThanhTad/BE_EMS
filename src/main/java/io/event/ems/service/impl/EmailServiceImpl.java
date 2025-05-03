package io.event.ems.service.impl;

import java.nio.charset.StandardCharsets;

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
    public void sendQrCode(String toMail, String fullName, byte[] qrCodeImage) {
        log.info("Attempting to send QR Code email to {}", toMail);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);

            Context context = new Context();
            context.setVariable("name", fullName);
            context.setVariable("qrImageCid", "qrImage");

            String html = templateEngine.process("email-qr-code", context);

            helper.setTo(toMail);
            helper.setSubject("Vé tham gia sự kiện của bạn");
            helper.setText(html, true);
            helper.addInline("qrImage", new ByteArrayResource(qrCodeImage), "image/png");

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Không thể gửi email với mã QR", e);
        }
    }

}
