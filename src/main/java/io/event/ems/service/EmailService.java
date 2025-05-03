package io.event.ems.service;

public interface EmailService {

    void sendOtpEmail(String toMail, String subject, String otp);

    void sendQrCode(String toMail, String fullName, byte[] qrCodeImage);

}
