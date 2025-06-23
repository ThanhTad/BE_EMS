package io.event.ems.service;

import java.util.List;
import java.util.Map;

public interface EmailService {

    void sendOtpEmail(String toMail, String subject, String otp);

    void sendGroupTicketConfirmation(String toEmail, String fullName, String transactionId, String eventName, List<Map<String, Object>> ticketDetails, Map<String, byte[]> inlineQrImages);

    public void sendWelcomeEmail(String to, String username);

}
