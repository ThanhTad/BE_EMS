package io.event.ems.service.impl;

import java.time.LocalDateTime;

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
import io.event.ems.dto.ResetPasswordRequest;
import io.event.ems.dto.SentOtpRequest;
import io.event.ems.dto.TokenResponse;
import io.event.ems.dto.VerifyOtpRequest;
import io.event.ems.exception.AuthException;
import io.event.ems.exception.DuplicateEmailException;
import io.event.ems.exception.DuplicateUsernameException;
import io.event.ems.exception.OtpException;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.exception.UnauthorizedException;
import io.event.ems.mapper.UserMapper;
import io.event.ems.model.Role;
import io.event.ems.model.StatusCode;
import io.event.ems.model.User;
import io.event.ems.model.UserSettings;
import io.event.ems.repository.StatusCodeRepository;
import io.event.ems.repository.UserRepository;
import io.event.ems.security.CustomUserDetails;
import io.event.ems.security.jwt.JwtService;
import io.event.ems.security.otp.OtpService;
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

    private static final String OTP_TYPE_2FA = "2FA";
    private static final String OTP_TYPE_PWD_RESET = "PWD_RESET";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_LOCKED = "LOCKED";

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

            checkUserStatus(user, STATUS_ACTIVE, "Account is not active or locked");

            if (user.getTwoFactorEnabled()) {
                return handleTwoFactorRequired(user.getEmail(), user.getTwoFactorEnabled());
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
            StatusCode activeStatus = statusCodeRepository.findByEntityTypeAndStatus("USER", "ACTIVE")
                    .orElseThrow(() -> new ResourceNotFoundException("Default status not found"));
            user.setStatus(activeStatus);

            UserSettings userSettings = new UserSettings(user);
            user.setSettings(userSettings);

            userRepository.save(user);
            log.info("User registered successfully: {}", user.getUsername());
            emailService.sendWelcomeEmail(user.getEmail(), user.getUsername());
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

    private void updateLastLogin(User user) {
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        log.debug("Updated last login for user: {}", user.getUsername());
    }

    private TokenResponse handleTwoFactorRequired(String userEmail, boolean isTwoFactorEnabled) {
        if (isTwoFactorEnabled) {
            log.info("2FA required for user associated with {}. Sending OTP.", userEmail);
            try {
                String otp = otpService.generateAndStoreOtp(userEmail, OTP_TYPE_2FA);
                emailService.sendOtpEmail(userEmail, "Your EMS 2FA Code", otp);
                return TokenResponse.builder()
                        .twoFactorEnabled(true)
                        .build();
            } catch (OtpException | MailException e) {
                log.error("Failed to generate or send 2FA OTP for {}: {}", userEmail, e.getMessage());
                throw new AuthException("Failed to send 2FA code. Please try logging in again.");
            }
        }
        log.warn("handleTwoFactorRequired called but 2FA is not enabled for {}", userEmail);
        throw new AuthException("Internal server error during login.");
    }

    private void checkUserStatus(User user, String expectedStatus, String errorMessage) {
        if (user.getStatus() == null || !expectedStatus.equalsIgnoreCase(user.getStatus().getStatus())) {
            log.warn("User status check failed for {}. Expected: {}, Actual: {}",
                    user.getUsername(), expectedStatus,
                    user.getStatus() != null ? user.getStatus().getStatus() : "null");
            if (user.getStatus() != null && STATUS_LOCKED.equalsIgnoreCase(user.getStatus().getStatus())) {
                throw new AuthException("Account is looked.");
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
    @Transactional(readOnly = true)
    public void requestPasswordReset(RequestPasswordResetRequest request) {
        String email = request.getEmail();
        log.info("Password reset requested for email: {}", email);
        userRepository.findByEmail(email).ifPresent(user -> {
            if (isUserStatusActive(user)) {
                try {
                    String otp = otpService.generateAndStoreOtp(user.getEmail(), OTP_TYPE_PWD_RESET);
                    emailService.sendOtpEmail(user.getEmail(), "Your EMS Password Reset Code", otp);
                    log.info("Password reset OTP sent to user {}", user.getUsername());
                } catch (OtpException | MailException e) {
                    log.error("Failed to send password reset OTP for {}: {}", user.getEmail(), e.getMessage());
                } catch (Exception e) {
                    log.error("Unexpected error during password reset OTP generation/sending for {}: {}",
                            user.getEmail(), e.getMessage(), e);
                }
            } else {
                log.warn("Password reset requested for inactive/locked user associated with email: {}", email);
            }
        });

    }

    private boolean isUserStatusActive(User user) {
        return user.getStatus() != null && STATUS_ACTIVE.equalsIgnoreCase(user.getStatus().getStatus());
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail();
        log.info("Attempting to reset password for email: {}", email);
        validateOtp(email, OTP_TYPE_PWD_RESET, request.getOtp());

        User user = findUserByUsername(email);

        checkUserStatus(user, STATUS_ACTIVE, "Cannot reset password for inactive account");

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password reset successfully for user {}", user.getUsername());
    }

    private void validateOtp(String identifier, String otpType, String otp) {
        try {
            boolean isValid = otpService.validateOtp(identifier, otpType, otp);
            if (!isValid) {
                throw new OtpException("Invalid OTP provided.");
            }
            log.debug("OTP validation successful for identifier: {}, type: {}", identifier, otpType);
        } catch (OtpException e) {
            log.warn("OTP validation failed for identifier: {}, type: {}. Reason: {}", identifier, otpType,
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
        log.info("Enabling 2FA for user: {}", username);
        User user = findUserByUsername(username);

        checkUserStatus(user, STATUS_ACTIVE, "Cannot enable 2FA for inactive account");

        if (user.getTwoFactorEnabled()) {
            throw new AuthException("2FA is already enabled for this account.");
        }

        user.setTwoFactorEnabled(true);
        userRepository.save(user);
        log.info("2FA enabled successfully for user {}", user.getUsername());
    }

    @Override
    public void disableTwoFactorAuth(Disable2FARequest request) {
        String username = request.username();
        String otp = request.otp();
        log.info("Disabling 2FA For user {}", username);
        User user = findUserByUsername(username);

        checkUserStatus(user, STATUS_ACTIVE, "Cannot disable 2FA for inactive account");

        if (!user.getTwoFactorEnabled()) {
            log.warn("Attempted to disable 2FA for user associated with email {} but it was not enabled.",
                    user.getEmail());
            return;
        }

        log.debug("Verifying OTP to disable 2FA for user associated with email: {}", user.getEmail());
        validateOtp(username, OTP_TYPE_2FA, otp);

        user.setTwoFactorEnabled(false);
        userRepository.save(user);
        log.info("2FA disabled successfully for user {}", user.getUsername());
    }

    @Override
    public void sendOtpToDisable2FA(SentOtpRequest request) {
        String username = request.username();
        log.info("Sending OTP to disable 2FA for user {}", username);
        User user = findUserByUsername(username);
        checkUserStatus(user, STATUS_ACTIVE, "Cannot send otp for inactive account");

        if (!user.getTwoFactorEnabled()) {
            log.warn("User {} tried to request disable-2FA OTP but 2FA is not enabled.", user.getUsername());
            throw new IllegalArgumentException("2FA is not enabled for this account.");
        }

        otpService.generateAndStoreOtp(username, OTP_TYPE_2FA);
    }

    @Override
    @Transactional
    public TokenResponse verifyTwoFactorOtp(String authorizationHeader, VerifyOtpRequest request,
            HttpServletResponse response) {
        String accessToken = jwtService.extractValidatedToken(authorizationHeader);

        String usernameFromToken = jwtService.extractUsername(accessToken);
        String username = request.getIdentifier();

        if (!username.equals(usernameFromToken)) {
            log.warn("Token subject does not match the OTP identifier. Token user: {}, Identifier: {}",
                    usernameFromToken, username);
            throw new UnauthorizedException("Access token does not belong to the specified identifier.");
        }

        log.info("Verifying 2FA OTP for identifier: {}", username);

        validateOtp(username, OTP_TYPE_2FA, request.getOtp());

        User user = findUserByUsername(username);

        checkUserStatus(user, STATUS_ACTIVE, "Cannot verify for inactive account");

        updateLastLogin(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        log.info("2FA verification successful for user {}. Tokens generated.", user.getUsername());

        // Set accessToken và refreshToken vào cookie khi 2FA thành công
        String newAccessToken = jwtService.generateAccessToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails, user.getId());
        cookieUtil.createAccessTokenCookie(response, newAccessToken);
        cookieUtil.createRefreshTokenCookie(response, newRefreshToken);

        return createTokenResponse(newAccessToken, user);
    }

    @Override
    @Transactional(readOnly = true)
    public void resendOtp(ResendOtpRequest request) {
        log.info("Resend OTP request received for identifier: {}, type: {}", request.username(), request.otpType());

        String username = request.username();
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            log.warn("Resend OTP requested for non-existent user {}", username);
            return;
        }
        if (OTP_TYPE_2FA.equalsIgnoreCase(request.otpType()) && !user.getTwoFactorEnabled()) {
            log.warn("Resend 2FA OTP requested for user {} but 2FA is not enabled", username);
            return;
        }

        try {
            String otp = otpService.generateAndStoreOtp(username, request.otpType());

            String subject;
            switch (request.otpType()) {
                case OTP_TYPE_2FA:
                    subject = "Your New EMS 2FA Code";
                    break;

                case OTP_TYPE_PWD_RESET:
                    subject = "Your New EMS Password Reset Code";
                    break;

                default:
                    log.error("Invalid OTP type '{}' encountered during resend", request.otpType());
                    return;
            }

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

}