package io.event.ems.service;

import io.event.ems.dto.Disable2FARequest;
import io.event.ems.dto.Enable2FARequest;
import io.event.ems.dto.LoginRequestDTO;
import io.event.ems.dto.RegisterRequestDTO;
import io.event.ems.dto.RequestPasswordResetRequest;
import io.event.ems.dto.ResendOtpRequest;
import io.event.ems.dto.ResetPasswordVerificationResponse;
import io.event.ems.dto.SentOtpRequest;
import io.event.ems.dto.TokenResponse;
import io.event.ems.dto.TwoFactorVerificationRequest;
import jakarta.servlet.http.HttpServletResponse;

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

    TokenResponse getUserInfo(String accessToken);

    void verifyEmailOtp(String email, String otp);

}
