package io.event.ems.service;

import io.event.ems.dto.EmailDetails;

public interface EmailService {

    void sendOtpEmail(String toMail, String subject, String otp);

    void sendPurchaseConfirmationEmail(EmailDetails emailDetails);

    void sendWelcomeEmail(String to, String username);

}
