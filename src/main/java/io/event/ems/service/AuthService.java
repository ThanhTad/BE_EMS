package io.event.ems.service;

import io.event.ems.dto.Disable2FARequest;
import io.event.ems.dto.Enable2FARequest;
import io.event.ems.dto.LoginRequestDTO;
import io.event.ems.dto.RefreshTokenRequest;
import io.event.ems.dto.RegisterRequestDTO;
import io.event.ems.dto.RequestPasswordResetRequest;
import io.event.ems.dto.ResendOtpRequest;
import io.event.ems.dto.ResetPasswordRequest;
import io.event.ems.dto.SentOtpRequest;
import io.event.ems.dto.TokenResponse;
import io.event.ems.dto.VerifyOtpRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    TokenResponse login(LoginRequestDTO request, HttpServletResponse response);

    TokenResponse refreshToken(RefreshTokenRequest request, HttpServletResponse response);

    void requestPasswordReset(RequestPasswordResetRequest request);

    void resetPassword(ResetPasswordRequest request);

    void logout(RefreshTokenRequest refreshToken, HttpServletResponse response);

    void enableTwoFactorAuth(Enable2FARequest request);

    void disableTwoFactorAuth(Disable2FARequest request);

    void sendOtpToDisable2FA(SentOtpRequest request);

    TokenResponse verifyTwoFactorOtp(String authorizationHeader, VerifyOtpRequest request,
            HttpServletResponse response);

    void resendOtp(ResendOtpRequest request);

    void register(RegisterRequestDTO request);

}
