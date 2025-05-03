package io.event.ems.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.event.ems.dto.ApiResponse;
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
import io.event.ems.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/auth/v1")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs for user authentication, registration, 2FA, and password management")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login with usernam and password")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequestDTO request) {
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/rf-token")
    @Operation(summary = "Refresh the access token using a valid refresh token")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/pass-reset/request")
    @Operation(summary = "Request a password reset OTP to be sent via email")
    public ResponseEntity<ApiResponse<Void>> requestResetPassword(
            @Valid @RequestBody RequestPasswordResetRequest request) {
        authService.requestPasswordReset(request);
        return ResponseEntity.accepted().body(
                ApiResponse.accepted("If an account exists for this email, a password reset code has been sent."));
    }

    @PostMapping("/pass-reset/confirm")
    @Operation(summary = "Confirm password reset using the OTP and set a new password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password has been reset successfully", null));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout the user by invalidating the refresh token")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/2fa/enable")
    @Operation(summary = "Enable Two-Factor Auhthentication for the current user")
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

    @PostMapping("/2fa/disable/sent-otp")
    @Operation(summary = "Sent OTP for disable 2fa")
    public ResponseEntity<ApiResponse<Void>> sentOTPToDisable2FA(@Valid @RequestParam SentOtpRequest request) {
        authService.sendOtpToDisable2FA(request);
        return ResponseEntity.ok(ApiResponse.success("OTP has been sent", null));
    }

    @PostMapping("/2fa/verify")
    @Operation(summary = "Verify 2FA OTP after successful password login")
    public ResponseEntity<ApiResponse<TokenResponse>> verify(@RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody VerifyOtpRequest request) {
        TokenResponse response = authService.verifyTwoFactorOtp(authorizationHeader, request);
        return ResponseEntity.ok(ApiResponse.success(response));

    }

    @PostMapping("/otp/resend")
    @Operation(summary = "Resend an OTP (for 2FA or Password Reset)")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        authService.resendOtp(request);
        return ResponseEntity.accepted().body(ApiResponse.accepted("If your request is valid, a new OTP will be sent"));
    }

}
