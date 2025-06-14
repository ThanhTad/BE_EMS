package io.event.ems.service;

import java.util.List;
import java.util.Map;

public interface EmailService {

    void sendOtpEmail(String toMail, String subject, String otp);

    void sendGroupTicketConfirmation(String toMail, String fullName, String transactionId,
            List<Map<String, Object>> tickets, Map<String, byte[]> inlineQrImages);

    public void sendWelcomeEmail(String to, String username);

}
