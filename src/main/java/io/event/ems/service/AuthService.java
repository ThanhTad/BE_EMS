package io.event.ems.service;

import io.event.ems.dto.*;
import jakarta.servlet.http.HttpServletResponse;

import java.util.UUID;

public interface AuthService {

    TokenResponse login(LoginRequestDTO request, HttpServletResponse response);

    TokenResponse refreshToken(String request, HttpServletResponse response);

    void requestPasswordReset(RequestPasswordResetRequest request);

    void resetPasswordWithToken(String email, String resetToken, String newPassword);

    ResetPasswordVerificationResponse verifyPasswordResetOtp(String email, String otp);

    void logout(String refreshToken, HttpServletResponse response);

    void sendOtpToEnable2FA(SentOtpRequest request);

    void enableTwoFactorAuth(Enable2FARequest request);

    void disableTwoFactorAuth(Disable2FARequest request);

    TokenResponse sendOtpToDisable2FA(SentOtpRequest request);

    TokenResponse verifyTwoFactorOtp(TwoFactorVerificationRequest request,
                                     HttpServletResponse response);

    void resendOtp(ResendOtpRequest request);

    void register(RegisterRequestDTO request);

    UserResponseDTO getUserInfo(UUID userId);

    void verifyEmailOtp(String email, String otp);

}
