package io.event.ems.controller;

import io.event.ems.dto.*;
import io.event.ems.security.CustomUserDetails;
import io.event.ems.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "APIs for user authentication, registration, 2FA, and password management")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login with username and password")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequestDTO request,
            HttpServletResponse response) {
        TokenResponse tokenResponse = authService.login(request, response);
        return ResponseEntity.ok(ApiResponse.success(tokenResponse));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequestDTO request) {
        authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Registration successful", null));
    }

    @PostMapping("/rf-token")
    @Operation(summary = "Refresh the access token using a valid refresh token")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {
        TokenResponse tokenResponse = authService.refreshToken(refreshToken, response);
        return ResponseEntity.ok(ApiResponse.success(tokenResponse));
    }

    @PostMapping("/pass-reset/request")
    @Operation(summary = "Request a password reset OTP to be sent via email")
    public ResponseEntity<ApiResponse<Void>> requestResetPassword(
            @Valid @RequestBody RequestPasswordResetRequest request) {
        authService.requestPasswordReset(request);
        return ResponseEntity.accepted().body(
                ApiResponse.accepted("If an account exists for this email, a password reset code has been sent."));
    }

    @PostMapping("/pass-reset/verify-otp")
    @Operation(summary = "Verify password reset OTP and get reset token")
    public ResponseEntity<ApiResponse<ResetPasswordVerificationResponse>> verifyPasswordResetOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        ResetPasswordVerificationResponse response = authService.verifyPasswordResetOtp(
                request.getIdentifier(), request.getOtp());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/pass-reset/confirm")
    @Operation(summary = "Reset password using reset token")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordWithTokenRequest request) {
        authService.resetPasswordWithToken(request.getEmail(), request.getResetToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Password has been reset successfully", null));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout the user by invalidating the refresh token")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {
        authService.logout(refreshToken, response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/2fa/enable")
    @Operation(summary = "Enable Two-Factor Authentication for the current user")
    public ResponseEntity<ApiResponse<Void>> enable2FA(@Valid @RequestBody Enable2FARequest request) {
        authService.enableTwoFactorAuth(request);
        return ResponseEntity.ok(ApiResponse.success("2FA has been enabled", null));
    }

    @PostMapping("/2fa/disable")
    @Operation(summary = "Disable 2FA for the current user", description = "Requires a valid 2FA OTP to confirm")
    public ResponseEntity<ApiResponse<Void>> disable2FA(@Valid @RequestBody Disable2FARequest request) {
        authService.disableTwoFactorAuth(request);
        return ResponseEntity.ok(ApiResponse.success("2FA has been disabled", null));
    }

    @PostMapping("/2fa/enable/sent-otp")
    @Operation(summary = "Send OTP for enabling 2FA")
    public ResponseEntity<ApiResponse<Void>> sentOTPToEnable2FA(@Valid @RequestBody SentOtpRequest request) {
        authService.sendOtpToEnable2FA(request);
        return ResponseEntity.ok(ApiResponse.success("OTP has been sent", null));
    }

    @PostMapping("/2fa/disable/sent-otp")
    @Operation(summary = "Send OTP for disable 2fa")
    public ResponseEntity<ApiResponse<Void>> sentOTPToDisable2FA(@Valid @RequestBody SentOtpRequest request) {
        authService.sendOtpToDisable2FA(request);
        return ResponseEntity.ok(ApiResponse.success("OTP has been sent", null));
    }

    @PostMapping("/2fa/verify")
    @Operation(summary = "Verify 2FA OTP after successful password login")
    public ResponseEntity<ApiResponse<TokenResponse>> verify(
            @Valid @RequestBody TwoFactorVerificationRequest request,
            HttpServletResponse response) {
        TokenResponse tokenResponse = authService.verifyTwoFactorOtp(request, response);
        return ResponseEntity.ok(ApiResponse.success(tokenResponse));
    }

    @PostMapping("/otp/resend")
    @Operation(summary = "Resend an OTP (for 2FA or Password Reset)")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        authService.resendOtp(request);
        return ResponseEntity.accepted().body(ApiResponse.accepted("If your request is valid, a new OTP will be sent"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getMe(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        UserResponseDTO userInfo = authService.getUserInfo(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(userInfo));
    }

    @PostMapping("/email/verify")
    @Operation(summary = "Verify email using OTP after registration")
    public ResponseEntity<ApiResponse<Void>> verifyEmailOtp(
            @RequestBody VerifyOtpRequest verifyOtpRequest) {
        authService.verifyEmailOtp(verifyOtpRequest.getIdentifier(), verifyOtpRequest.getOtp());
        return ResponseEntity.ok(ApiResponse.success("Email has been verified successfully", null));
    }

}