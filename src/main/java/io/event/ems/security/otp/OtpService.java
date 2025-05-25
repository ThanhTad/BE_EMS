package io.event.ems.security.otp;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.event.ems.exception.OtpException;
import io.event.ems.service.RedisService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class OtpService {

    private final RedisService redisService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${ems.security.otp.length}")
    private int otpLength;

    @Value("${ems.security.otp.validity-minutes}")
    private long otpValidityMinutes;

    @Value("${ems.security.otp.max-attempts}")
    private int maxAttempts;

    @Value("${ems.security.otp.lockout-duration-minutes}")
    private long lockoutDurationMinutes;

    @Value("${ems.security.otp.resend-delay-seconds}")
    private long resendDelaySeconds;

    @Value("${ems.security.otp.max-resend-requests-per-hour}")
    private long maxResendRequestsPerHour;

    // Redis Key Prefixes
    private static final String OTP_VALUE_PREFIX = "otp:value:";
    private static final String OTP_ATTEMPT_PREFIX = "otp:attempt:";
    private static final String OTP_LOCKOUT_PREFIX = "otp:lockout:";
    private static final String OTP_RESEND_DELAY_PREFIX = "otp:resend_delay:";
    private static final String OTP_RESEND_HOURLY_COUNT_PREFIX = "otp:resend_hourly:";

    // Time Constants
    private static final long ONE_HOUR_IN_SECONDS = TimeUnit.HOURS.toSeconds(1);
    private static final long ONE_MINUTE_IN_SECONDS = TimeUnit.MINUTES.toSeconds(1);

    @PostConstruct
    public void validateConfig() {
        if (otpLength < 4 || otpLength > 8) {
            log.error("Invalid OTP length configuration: {}. Must be between 4 and 8", otpLength);
            throw new IllegalArgumentException("OTP length must be between 4 and 8 digits");
        }
        if (maxAttempts <= 0) {
            log.error("Invalid max attempts configuration: {}. Must be positive.", maxAttempts);
            throw new IllegalArgumentException("Max attempts must be positive");
        }
        if (otpValidityMinutes <= 0) {
            log.error("Invalid OTP validity configuration: {}. Must be positive.", otpValidityMinutes);
            throw new IllegalArgumentException("OTP validity (minutes) must be positive");
        }
        if (lockoutDurationMinutes <= 0) {
            log.error("Invalid lockout duration configuration: {}. Must be positive.", lockoutDurationMinutes);
            throw new IllegalArgumentException("Lockout duration (minutes) must be positive");
        }
        if (resendDelaySeconds < 0) {
            log.error("Invalid resend delay configuration: {}. Cannot be negative.", resendDelaySeconds);
            throw new IllegalArgumentException("Resend delay (seconds) cannot be negative");
        }
        if (maxResendRequestsPerHour <= 0) {
            log.error("Invalid max resend requests per hour configuration: {}. Must be positive.",
                    maxResendRequestsPerHour);
            throw new IllegalArgumentException("Max resend requests per hour must be positive");
        }
        log.info(
                "OTP Service configured: length={}, validity-min={}, maxAttempts={}, lockout-min={}, resendDelay-s={}, maxResend/hr={}",
                otpLength, otpValidityMinutes, maxAttempts, lockoutDurationMinutes, resendDelaySeconds,
                maxResendRequestsPerHour);
    }

    public String generateAndStoreOtp(String identifier, String otpType) {
        String baseKey = otpType + ":" + identifier;
        String lockoutKey = OTP_LOCKOUT_PREFIX + baseKey;
        String resendDelayKey = OTP_RESEND_DELAY_PREFIX + baseKey;
        long currentHour = Instant.now().getEpochSecond() / ONE_HOUR_IN_SECONDS;
        String hourlyCountKey = OTP_RESEND_HOURLY_COUNT_PREFIX + baseKey + ":" + currentHour;

        checkLockoutStatus(lockoutKey, otpType, identifier);
        checkResendDelay(resendDelayKey, otpType, identifier);
        checkAndIncrementHourlyLimit(hourlyCountKey, otpType, identifier);

        // Invalidate any existing OTP before issuing new one
        invalidateOtp(identifier, otpType);

        String otp = generateOtpString();
        String valueKey = OTP_VALUE_PREFIX + baseKey;
        long otpValiditySeconds = otpValidityMinutes * ONE_MINUTE_IN_SECONDS;
        redisService.setValue(valueKey, otp, otpValiditySeconds);
        log.debug("Stored new OTP for {}:{} with validity {} seconds", otpType, identifier, otpValiditySeconds);

        resetAttemptCounter(baseKey, otpValiditySeconds);

        if (resendDelaySeconds > 0) {
            redisService.setValue(resendDelayKey, "active", resendDelaySeconds);
            log.debug("Set resend delay for {}:{} for {} seconds", otpType, identifier, resendDelaySeconds);
        }
        log.info("Generated OTP successfully for {}:{}", otpType, identifier);
        return otp;
    }

    public boolean validateOtp(String identifier, String otpType, String otp) {
        String baseKey = otpType + ":" + identifier;
        String valueKey = OTP_VALUE_PREFIX + baseKey;
        String attemptKey = OTP_ATTEMPT_PREFIX + baseKey;
        String lockoutKey = OTP_LOCKOUT_PREFIX + baseKey;

        checkLockoutStatus(lockoutKey, otpType, identifier);
        String storedOtp = (String) redisService.getValue(valueKey);

        if (storedOtp == null) {
            log.warn("OTP validation failed for {}:{} - not found or expired", otpType, identifier);
            handleInvalidAttempt(identifier, otpType, attemptKey, lockoutKey);
            throw new OtpException("OTP is invalid or has expired. Please request a new one");
        }

        if (storedOtp.equals(otp)) {
            handleSuccessfulValidation(identifier, otpType, valueKey, attemptKey);
            return true;
        } else {
            log.warn("OTP validation failed for {}:{} - incorrect OTP", otpType, identifier);
            handleInvalidAttempt(identifier, otpType, attemptKey, lockoutKey);
            return false;
        }
    }

    public void invalidateOtp(String identifier, String otpType) {
        String baseKey = otpType + ":" + identifier;
        String key = OTP_VALUE_PREFIX + baseKey;
        redisService.deleteValue(key);
        log.debug("Invalidated OTP for {}:{}", otpType, identifier);
    }

    private void checkLockoutStatus(String lockoutKey, String otpType, String identifier) {
        if (redisService.getValue(lockoutKey) != null) {
            long ttl = redisService.getTtl(lockoutKey);
            long mins = (ttl + ONE_MINUTE_IN_SECONDS - 1) / ONE_MINUTE_IN_SECONDS;
            log.warn("Lockout active for {}:{} remaining {} seconds", otpType, identifier, ttl);
            throw new OtpException("Too many failed attempts. Try again in " + mins + " minute(s).");
        }
    }

    private void checkResendDelay(String resendDelayKey, String otpType, String identifier) {
        if (resendDelaySeconds > 0 && redisService.getValue(resendDelayKey) != null) {
            long remaining = redisService.getTtl(resendDelayKey);
            log.warn("Resend delay active for {}:{} remaining {} seconds", otpType, identifier, remaining);
            throw new OtpException("Please wait " + remaining + " second(s) before requesting a new code");
        }
    }

    private void checkAndIncrementHourlyLimit(String hourlyCountKey, String otpType, String identifier) {
        long newCount = redisService.incrementValue(hourlyCountKey);
        if (newCount == 1) {
            redisService.expire(hourlyCountKey, ONE_HOUR_IN_SECONDS);
        }
        if (newCount > maxResendRequestsPerHour) {
            log.warn("Hourly OTP limit exceeded for {}:{} count={}", otpType, identifier, newCount);
            throw new OtpException(
                    "You have reached the maximum limit of " + maxResendRequestsPerHour + " OTP requests per hour.");
        }
        log.debug("Hourly OTP request count for {}:{} is now {}/{}", otpType, identifier, newCount,
                maxResendRequestsPerHour);
    }

    private void resetAttemptCounter(String baseKey, long otpValiditySeconds) {
        String attemptKey = OTP_ATTEMPT_PREFIX + baseKey;
        redisService.setValue(attemptKey, 0, otpValiditySeconds + ONE_MINUTE_IN_SECONDS);
        log.debug("Reset attempt counter for {}", baseKey);
    }

    private void handleSuccessfulValidation(String identifier, String otpType, String valueKey, String attemptKey) {
        redisService.deleteValue(valueKey);
        redisService.deleteValue(attemptKey);
        log.info("Successful OTP validation for {}:{} cleaned up keys", otpType, identifier);
    }

    private void handleInvalidAttempt(String identifier, String otpType, String attemptKey, String lockoutKey) {
        long attempts = redisService.incrementValue(attemptKey);
        long ttl = redisService.getTtl(attemptKey);
        if (ttl < 0) {
            redisService.expire(attemptKey, otpValidityMinutes * ONE_MINUTE_IN_SECONDS + ONE_MINUTE_IN_SECONDS);
        }
        log.warn("Invalid OTP attempt {}/{} for {}:{}", attempts, maxAttempts, otpType, identifier);
        if (attempts >= maxAttempts) {
            long lockoutSec = lockoutDurationMinutes * ONE_MINUTE_IN_SECONDS;
            redisService.setValue(lockoutKey, "locked", lockoutSec);
            redisService.deleteValue(attemptKey);
            log.error("Account locked for {}:{} for {} seconds due to {} failed attempts", otpType, identifier,
                    lockoutSec, attempts);
            throw new OtpException(
                    "Too many failed attempts. Account locked for " + lockoutDurationMinutes + " minute(s).");
        }
    }

    private String generateOtpString() {
        StringBuilder sb = new StringBuilder(otpLength);
        for (int i = 0; i < otpLength; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();
    }
}