package io.event.ems.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

import org.springframework.mail.MailException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import io.event.ems.exception.AuthException;
import io.event.ems.exception.DuplicateEmailException;
import io.event.ems.exception.DuplicateUsernameException;
import io.event.ems.exception.OtpException;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.mapper.UserMapper;
import io.event.ems.model.Role;
import io.event.ems.model.StatusCode;
import io.event.ems.model.User;
import io.event.ems.model.UserSettings;
import io.event.ems.repository.StatusCodeRepository;
import io.event.ems.repository.UserRepository;
import io.event.ems.security.CustomUserDetails;
import io.event.ems.security.jwt.JwtService;
import io.event.ems.security.otp.ChallengeTokenService;
import io.event.ems.security.otp.OtpService;
import io.event.ems.security.otp.ResetTokenService;
import io.event.ems.service.AuthService;
import io.event.ems.service.EmailService;
import io.event.ems.util.CookieUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final OtpService otpService;
    private final EmailService emailService;
    private final UserDetailsServiceImpl userDetailsService;
    private final UserMapper mapper;
    private final StatusCodeRepository statusCodeRepository;
    private final CookieUtil cookieUtil;
    private final ResetTokenService resetTokenService;
    private final ChallengeTokenService challengeTokenService;

    private static final String OTP_TYPE_2FA_LOGIN = "2FA_LOGIN";
    private static final String OTP_TYPE_2FA_ENABLE = "2FA_ENABLE";
    private static final String OTP_TYPE_2FA_DISABLE = "2FA_DISABLE";
    private static final String OTP_TYPE_EMAIL_VERIFY = "EMAIL_VERIFY";
    private static final String OTP_TYPE_PWD_RESET = "PWD_RESET";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_LOCKED = "LOCKED";
    private static final String STATUS_UNVERIFIED = "UNVERIFIED";

    @Override
    @Transactional
    public TokenResponse login(LoginRequestDTO request, HttpServletResponse response) {
        log.info("Login attempt for username:{}", request.getUsername());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();

            if (user.getStatus() == null || STATUS_UNVERIFIED.equalsIgnoreCase(user.getStatus().getStatus())) {
                log.warn("Login failed for username '{}': Account is not verified", request.getUsername());
                throw new AuthException("Account is not verified. Please check your email.");
            }
            // Kiểm tra trạng thái người dùng
            checkUserStatus(user, STATUS_ACTIVE, "Account is not active or locked");

            if (user.getTwoFactorEnabled()) {
                return handleTwoFactorRequired(user.getEmail(), user.getTwoFactorEnabled(), OTP_TYPE_2FA_LOGIN);
            }

            updateLastLogin(user);

            String jwtToken = jwtService.generateAccessToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails, user.getId());
            cookieUtil.createAccessTokenCookie(response, jwtToken);
            cookieUtil.createRefreshTokenCookie(response, refreshToken);

            log.info("User {} logged in successfully (2FA not required)", user.getUsername());
            return createTokenResponse(jwtToken, user);
        } catch (BadCredentialsException e) {
            log.warn("Login failed for username '{}': Invalid credentials", request.getUsername());
            throw new AuthException("Invalid username or password");
        } catch (LockedException e) {
            log.warn("Login failed for username '{}': Account locked", request.getUsername());
            throw new AuthException("Account is locked. Please contact support.");
        } catch (DisabledException e) {
            log.warn("Login failed for username '{}': Account disabled/inactive", request.getUsername());
            throw new AuthException("Account is not active. Please verify your email or contact support.");
        } catch (AuthException ae) {
            throw ae;
        } catch (OtpException | MailException oe) {
            log.error("OTP sending error during login for {}: {}", request.getUsername(), oe.getMessage());
            throw new AuthException("Could not send 2FA code. Please try again.");
        } catch (Exception e) {
            log.error("An unexpected error occurred during login for {}: {}", request.getUsername(), e.getMessage(), e);
            throw new AuthException("Login failed due to an unexpected error.");
        }
    }

    @Override
    @Transactional
    public void register(RegisterRequestDTO request) {
        log.info("Processing registration request for username: {}", request.getUsername());
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed: Username {} already exists", request.getUsername());
            throw new DuplicateUsernameException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: Email {} already exists", request.getEmail());
            throw new DuplicateEmailException("Email already exists");
        }
        try {
            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setFullName(request.getFullName());
            user.setPhone(request.getPhone());
            user.setCreatedAt(LocalDateTime.now());
            user.setTwoFactorEnabled(false);
            user.setRole(Role.USER);

            StatusCode unverifiedStatus = statusCodeRepository.findByEntityTypeAndStatus("USER", STATUS_UNVERIFIED)
                    .orElseThrow(() -> new ResourceNotFoundException("Default status not found"));
            user.setStatus(unverifiedStatus);

            UserSettings userSettings = new UserSettings(user);
            user.setSettings(userSettings);
            userRepository.save(user);

            String otp = otpService.generateAndStoreOtp(user.getEmail(), OTP_TYPE_EMAIL_VERIFY);
            log.info("User registered successfully: {}", user.getUsername());
            emailService.sendOtpEmail(user.getEmail(), "Verify your EMS account", otp);
        } catch (MailException e) {
            log.error("Failed to send welcome email to {}: {}", request.getEmail(), e.getMessage());
        } catch (Exception e) {
            log.error("Registration failed for username {}: {}", request.getUsername(), e.getMessage(), e);
            throw new AuthException("Registration failed due to an unexpected error");
        }
    }

    private TokenResponse createTokenResponse(
            String accessToken,
            User user) {
        Long expirationMillis = jwtService.extractClaim(accessToken, claims -> claims.getExpiration().getTime());

        return TokenResponse.builder()
                .accessTokenExpiresIn(expirationMillis)
                .twoFactorEnabled(user.getTwoFactorEnabled())
                .user(mapper.toResponseDTO(user))
                .build();
    }

    @Transactional
    private void updateLastLogin(User user) {
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        log.debug("Updated last login for user: {}", user.getUsername());
    }

    private TokenResponse handleTwoFactorRequired(String userEmail, boolean isTwoFactorEnabled, String otpType) {
        if (isTwoFactorEnabled) {
            log.info("2FA required for user associated with {}. Sending OTP.", userEmail);
            try {
                String challengeToken = challengeTokenService.generateChallengerToken(userEmail);

                String otp = otpService.generateAndStoreOtp(userEmail, otpType);
                String subject = getOtpEmailSubject(otpType);
                emailService.sendOtpEmail(userEmail, subject, otp);

                return TokenResponse.builder()
                        .twoFactorEnabled(true)
                        .challengeToken(challengeToken)
                        .build();
            } catch (OtpException | MailException e) {
                log.error("Failed to generate or send 2FA OTP for {}: {}", userEmail, e.getMessage());
                throw new AuthException("Failed to send 2FA code. Please try logging in again.");
            }
        }
        log.warn("handleTwoFactorRequired called but 2FA is not enabled for {}", userEmail);
        throw new AuthException("Internal server error during login.");
    }

    private String getOtpEmailSubject(String otpType) {
        return switch (otpType) {
            case OTP_TYPE_2FA_LOGIN -> "Your EMS 2FA Login Code";
            case OTP_TYPE_2FA_ENABLE -> "Your EMS 2FA Code";
            case OTP_TYPE_2FA_DISABLE -> "Your EMS 2FA Disable Code";
            case OTP_TYPE_PWD_RESET -> "Your EMS Password Reset Code";
            case OTP_TYPE_EMAIL_VERIFY -> "Your EMS Account Verification Code";
            default -> "Your EMS Verification Code";
        };
    }

    private void checkUserStatus(User user, String expectedStatus, String errorMessage) {
        if (user.getStatus() == null || !expectedStatus.equalsIgnoreCase(user.getStatus().getStatus())) {
            log.warn("User status check failed for {}. Expected: {}, Actual: {}",
                    user.getUsername(), expectedStatus,
                    user.getStatus() != null ? user.getStatus().getStatus() : "null");
            if (user.getStatus() != null && STATUS_LOCKED.equalsIgnoreCase(user.getStatus().getStatus())) {
                throw new AuthException("Account is locked.");
            }
            throw new AuthException(errorMessage);
        }
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }

    @Override
    public TokenResponse refreshToken(String refreshToken, HttpServletResponse response) {
        log.debug("Attempting to refresh token");
        try {
            String username = jwtService.extractUsername(refreshToken);
            if (username == null) {
                throw new AuthException("Invalid refresh token: Cannot extract username");
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (!jwtService.isTokenValid(refreshToken, userDetails)) {
                throw new AuthException("Invalid or expired refresh token.");
            }

            User user = findUserByUsername(username);

            String newAccessToken = jwtService.generateAccessToken(userDetails);
            // Rolling refresh token? Nếu có thì generate và set lại, nếu không thì chỉ set
            // access token.
            cookieUtil.createAccessTokenCookie(response, newAccessToken);
            cookieUtil.createRefreshTokenCookie(response, refreshToken);

            log.info("Access token refreshed successfully for user {}", username);

            return TokenResponse.builder()
                    .accessTokenExpiresIn(
                            jwtService.extractClaim(newAccessToken, claims -> claims.getExpiration().getTime()))
                    .twoFactorEnabled(user.getTwoFactorEnabled())
                    .user(mapper.toResponseDTO(user))
                    .build();

        } catch (ExpiredJwtException eje) {
            log.warn("Refresh token has expired: {}", eje.getMessage());
            throw new AuthException("Refresh token has expired. Please log in again.");
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid refresh token received: {}", e.getMessage());
            throw new AuthException("Invalid refresh token.");
        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage(), e);
            throw new AuthException("Could not refresh token. Please log in again.");
        }
    }

    @Override
    public void requestPasswordReset(RequestPasswordResetRequest request) {
        String email = request.getEmail();
        log.info("Password reset request for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!isUserStatusActive(user)) {
            throw new AuthException("Cannot reset password for inactive account");
        }

        try {
            String otp = otpService.generateAndStoreOtp(email, OTP_TYPE_PWD_RESET);
            emailService.sendOtpEmail(email, "Your EMS Password Reset Code", otp);
            log.info("Password reset OTP sent successfully to email: {}", email);
        } catch (Exception e) {
            log.error("Failed to send password reset OTP: {}", e.getMessage());
            throw new AuthException("Failed to send reset code. Please try again.");
        }
    }

    private boolean isUserStatusActive(User user) {
        return user.getStatus() != null && STATUS_ACTIVE.equalsIgnoreCase(user.getStatus().getStatus());
    }

    @Override
    @Transactional
    public void resetPasswordWithToken(String email, String resetToken, String newPassword) {
        log.info("Attempting to reset password for email: {}", email);

        if (!resetTokenService.validateResetToken(email, resetToken)) {
            log.warn("Invalid reset token provided for email: {}", email);
            throw new AuthException("Invalid or expired reset token.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        checkUserStatus(user, STATUS_ACTIVE, "Cannot reset password for inactive account");

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetTokenService.invalidateResetToken(email);

        log.info("Password reset successfully for user {}", user.getUsername());
    }

    private void validateOtp(String email, String otpType, String otp) {
        try {
            boolean isValid = otpService.validateOtp(email, otpType, otp);
            if (!isValid) {
                throw new OtpException("Invalid OTP provided.");
            }
            log.debug("OTP validation successful for email: {}, type: {}", email, otpType);
        } catch (OtpException e) {
            log.warn("OTP validation failed for email: {}, type: {}. Reason: {}", email, otpType,
                    e.getMessage());
            throw e;
        }
    }

    @Override
    public void logout(String refreshToken, HttpServletResponse response) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            log.warn("Logout attempt with missing refresh token.");
            return;
        }
        log.debug("Logout attempt");

        String jti = jwtService.extractJti(refreshToken);
        if (jti != null) {
            jwtService.blacklistRefreshToken(jti);
            log.info("User logged out. Refresh token blacklisted (JTI ending with: ...{})",
                    jti.substring(Math.max(0, jti.length() - 6)));
        } else {
            log.warn("Could not blacklist refresh token during logout: JTI not found in token.");
        }
        SecurityContextHolder.clearContext();

        // Xóa cả access token và refresh token cookie
        cookieUtil.deleteAccessTokenCookie(response);
        cookieUtil.deleteRefreshTokenCookie(response);
    }

    @Override
    @Transactional
    public void enableTwoFactorAuth(Enable2FARequest request) {
        String username = request.username();
        String otp = request.otp();

        log.info("Enabling 2FA for user: {}", username);
        User user = findUserByUsername(username);

        checkUserStatus(user, STATUS_ACTIVE, "Cannot enable 2FA for inactive account");

        if (user.getTwoFactorEnabled()) {
            throw new AuthException("2FA is already enabled for this account.");
        }

        if (otp != null && !otp.trim().isEmpty()) {
            log.debug("Verifying OTP to enable 2FA for user associated with email: {}", user.getEmail());
            validateOtp(user.getEmail(), OTP_TYPE_2FA_ENABLE, otp);
        } else {
            log.warn("No OTP provided for enabling 2FA for user associated with email: {}", user.getEmail());
            throw new AuthException("OTP is required to enable 2FA.");
        }

        user.setTwoFactorEnabled(true);
        userRepository.save(user);
        log.info("2FA enabled successfully for user {}", user.getUsername());
    }

    @Override
    @Transactional
    public void disableTwoFactorAuth(Disable2FARequest request) {
        String username = request.username();
        String otp = request.otp();

        log.info("Disabling 2FA For user {}", username);

        User user = findUserByUsername(username);
        checkUserStatus(user, STATUS_ACTIVE, "Cannot disable 2FA for inactive account");

        if (!user.getTwoFactorEnabled()) {
            log.warn("Attempted to disable 2FA for user associated with email {} but it was not enabled.",
                    user.getEmail());
            throw new AuthException("2FA is not enabled for this account.");
        }

        log.debug("Verifying OTP to disable 2FA for user associated with email: {}", user.getEmail());
        validateOtp(user.getEmail(), OTP_TYPE_2FA_DISABLE, otp);

        user.setTwoFactorEnabled(false);
        userRepository.save(user);
        log.info("2FA disabled successfully for user {}", user.getUsername());
    }

    @Override
    public TokenResponse sendOtpToDisable2FA(SentOtpRequest request) {
        String username = request.username();
        log.info("Sending OTP to disable 2FA for user {}", username);

        User user = findUserByUsername(username);
        checkUserStatus(user, STATUS_ACTIVE, "Cannot send otp for inactive account");

        if (!user.getTwoFactorEnabled()) {
            log.warn("User {} tried to request disable-2FA OTP but 2FA is not enabled.", user.getUsername());
            throw new IllegalArgumentException("2FA is not enabled for this account.");
        }

        try {
            String challengeToken = challengeTokenService.generateChallengerToken(user.getEmail());

            String otp = otpService.generateAndStoreOtp(user.getEmail(), OTP_TYPE_2FA_DISABLE);
            emailService.sendOtpEmail(user.getEmail(),
                    "Your EMS 2FA Disable Code",
                    otp);
            log.info("OTP sent successfully to disable 2FA for user {}", username);
            return TokenResponse.builder()
                    .challengeToken(challengeToken)
                    .build();
        } catch (Exception e) {
            log.error("Failed to send OTP to disable 2FA for user {}: {}", username, e.getMessage(), e);
            throw new AuthException("Failed to send OTP. Please try again.");
        }
    }

    @Override
    @Transactional
    public TokenResponse verifyTwoFactorOtp(TwoFactorVerificationRequest request,
            HttpServletResponse response) {
        String username = request.getIdentifier();
        String otp = request.getOtp();
        String challengeToken = request.getChallengeToken();

        log.info("Verifying 2FA OTP for identifier: {}", username);

        User user = findUserByUsername(username);

        if (challengeToken == null || !challengeTokenService.validateChallengeToken(user.getEmail(), challengeToken)) {
            log.warn("Invalid or missing challenge token for 2FA verification for user associated with email: {}",
                    username);
            throw new AuthException("Invalid or missing challenge token.");

        }

        log.info("Verifying 2FA OTP for identifier: {}", username);

        checkUserStatus(user, STATUS_ACTIVE, "Cannot verify for inactive account");

        validateOtp(user.getEmail(), OTP_TYPE_2FA_LOGIN, otp);

        updateLastLogin(user);

        log.info("2FA verification successful for user {}. Tokens generated.", user.getUsername());

        CustomUserDetails userDetails = new CustomUserDetails(user);

        // Set accessToken và refreshToken vào cookie khi 2FA thành công
        String newAccessToken = jwtService.generateAccessToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails, user.getId());
        cookieUtil.createAccessTokenCookie(response, newAccessToken);
        cookieUtil.createRefreshTokenCookie(response, newRefreshToken);

        log.info("2FA login verification successful for user: {}", user.getUsername());

        return createTokenResponse(newAccessToken, user);
    }

    @Override
    @Transactional(readOnly = true)
    public void resendOtp(ResendOtpRequest request) {
        log.info("Resend OTP request received for identifier: {}, type: {}", request.username(), request.otpType());

        String username = request.username();
        String otpType = request.otpType();
        String challengeToken = request.challengeToken();

        validateOtpType(otpType);

        User user = userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException(
                "User not found with username: " + username));

        validateOtpBusinessRules(user, otpType);

        if (requiresChallengeToken(otpType)) {
            if (challengeToken == null
                    || !challengeTokenService.validateChallengeToken(user.getEmail(), challengeToken)) {
                log.warn("Invalid or missing challenge token for resend OTP for user associated with email: {}",
                        user.getEmail());
                throw new AuthException("Invalid or missing challenge token.");
            }
        }

        try {
            String otp = otpService.generateAndStoreOtp(user.getEmail(), otpType);

            String subject = getOtpEmailSubject(otpType);

            emailService.sendOtpEmail(user.getEmail(), subject, otp);
            log.info("Resent OTP successfully for identifier: {}, type: {}", username, request.otpType());

        } catch (OtpException e) {
            log.warn("Failed to resend OTP for identifier: {}, type: {}, reason: {}", username, request.otpType(),
                    e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during OTP resend for identifier: {}, type: {}", username, request.otpType(),
                    e);
        }

    }

    private boolean requiresChallengeToken(String otpType) {
        return Arrays.asList(OTP_TYPE_2FA_LOGIN, OTP_TYPE_2FA_ENABLE, OTP_TYPE_2FA_DISABLE).contains(otpType);
    }

    private void validateOtpBusinessRules(User user, String otpType) {
        switch (otpType) {
            case OTP_TYPE_2FA_LOGIN, OTP_TYPE_2FA_DISABLE -> {
                if (!user.getTwoFactorEnabled()) {
                    throw new IllegalArgumentException("2FA is not enabled for this account.");
                }
            }
            case OTP_TYPE_2FA_ENABLE -> {
                if (user.getTwoFactorEnabled()) {
                    throw new IllegalArgumentException("2FA is already enabled for this account.");
                }
            }
            case OTP_TYPE_PWD_RESET, OTP_TYPE_EMAIL_VERIFY -> {
                // No additional validation needed
            }
            default -> throw new IllegalArgumentException("Unsupported OTP type: " + otpType);
        }
    }

    private void validateOtpType(String otpType) {
        if (!Arrays
                .asList(OTP_TYPE_2FA_LOGIN, OTP_TYPE_PWD_RESET, OTP_TYPE_EMAIL_VERIFY, OTP_TYPE_2FA_ENABLE,
                        OTP_TYPE_2FA_DISABLE)
                .contains(otpType)) {
            log.error("Invalid OTP type provided: {}", otpType);
            throw new IllegalArgumentException("Invalid OTP type provided.");
        }
    }

    @Override
    public TokenResponse getUserInfo(String accessToken) {
        String username = jwtService.extractUsername(accessToken);
        if (username == null) {
            throw new AuthException("Invalid or missing access token");
        }

        User user = findUserByUsername(username);

        Long expirationMillis = jwtService.extractClaim(accessToken, claims -> claims.getExpiration().getTime());

        return TokenResponse.builder()
                .accessTokenExpiresIn(expirationMillis)
                .twoFactorEnabled(user.getTwoFactorEnabled())
                .user(mapper.toResponseDTO(user))
                .build();
    }

    @Override
    @Transactional
    public void verifyEmailOtp(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        log.info("Verifying email OTP for user: {}", user.getUsername());
        if (user.getStatus() != null && STATUS_ACTIVE.equalsIgnoreCase(user.getStatus().getStatus())) {
            throw new AuthException("Email already verified");
        }
        validateOtp(email, OTP_TYPE_EMAIL_VERIFY, otp);
        StatusCode activeStatus = statusCodeRepository.findByEntityTypeAndStatus("USER", "ACTIVE")
                .orElseThrow(() -> new ResourceNotFoundException("Default status not found"));
        user.setStatus(activeStatus);
        user.setEmailVerified(true);
        userRepository.save(user);
        log.info("Email OTP verified successfully for user: {}", user.getUsername());
    }

    @Override
    public void sendOtpToEnable2FA(SentOtpRequest request) {
        String username = request.username();
        log.info("Sending OTP to enable 2FA for user {}", username);

        User user = findUserByUsername(username);
        checkUserStatus(user, STATUS_ACTIVE, "Cannot send otp for inactive account");

        if (user.getTwoFactorEnabled()) {
            log.warn("User {} tried to request enable-2FA OTP but 2FA is already enabled.", user.getUsername());
            throw new IllegalArgumentException("2FA is already enabled for this account.");
        }

        try {
            String otp = otpService.generateAndStoreOtp(user.getEmail(), OTP_TYPE_2FA_ENABLE);
            emailService.sendOtpEmail(user.getEmail(),
                    "Your EMS 2FA Code",
                    otp);
            log.info("OTP sent successfully to enable 2FA for user {}", username);
        } catch (Exception e) {
            log.error("Failed to send OTP to enable 2FA for user {}: {}", username, e.getMessage(), e);
            throw new AuthException("Failed to send OTP. Please try again.");
        }

    }

    @Override
    public ResetPasswordVerificationResponse verifyPasswordResetOtp(String email, String otp) {
        if (!otpService.validateOtp(email, OTP_TYPE_PWD_RESET, otp)) {
            log.warn("Invalid OTP provided for password reset verification for email: {}", email);
            throw new OtpException("Invalid OTP provided.");
        }

        log.info("Password reset OTP verified successfully for email: {}", email);

        String resetToken = generateSecureResetToken();
        resetTokenService.storeResetToken(email, resetToken, Duration.ofMinutes(10));

        return ResetPasswordVerificationResponse.builder()
                .resetToken(resetToken)
                .message("OTP verified successfully. Use the reset token to reset your password.")
                .expiresAt(System.currentTimeMillis() + Duration.ofMinutes(10).toMillis())
                .build();
    }

    private String generateSecureResetToken() {
        return UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
    }

}