package io.event.ems.service;

import io.event.ems.dto.Disable2FARequest;
import io.event.ems.dto.Enable2FARequest;
import io.event.ems.dto.LoginRequestDTO;
import io.event.ems.dto.RefreshTokenRequest;
import io.event.ems.dto.RequestPasswordResetRequest;
import io.event.ems.dto.ResendOtpRequest;
import io.event.ems.dto.ResetPasswordRequest;
import io.event.ems.dto.SentOtpRequest;
import io.event.ems.dto.TokenResponse;
import io.event.ems.dto.VerifyOtpRequest;

public interface AuthService {

    TokenResponse login(LoginRequestDTO request);

    TokenResponse refreshToken(RefreshTokenRequest request);

    void requestPasswordReset(RequestPasswordResetRequest request);

    void resetPassword(ResetPasswordRequest request);

    void logout(RefreshTokenRequest refreshToken);

    void enableTwoFactorAuth(Enable2FARequest request);

    void disableTwoFactorAuth(Disable2FARequest request);

    void sendOtpToDisable2FA(SentOtpRequest request);

    TokenResponse verifyTwoFactorOtp(String authorizationHeader, VerifyOtpRequest request);

    void resendOtp(ResendOtpRequest request);

}
