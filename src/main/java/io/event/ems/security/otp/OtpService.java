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
    public void validateConfig(){
        if(otpLength < 4 || otpLength > 8){
            log.error("Invalid OTP lenght configuration: {}. Must be between 4 and 8", otpLength);
            throw new IllegalArgumentException("OTP lenght must be between 4 and 8 digits");
        }

        if(maxAttempts <= 0){
            log.error("Invalid Mac attempts configuration: {}. Must be positive.", maxAttempts);
            throw new IllegalArgumentException("Max attempts must be positive");
        }

        if(otpValidityMinutes <= 0){
            log.error("Invalid OTP validity configuration: {}. Must be positive.", otpValidityMinutes);
            throw new IllegalArgumentException("OTP validity (minutes) must be positive");
        }

        if(lockoutDurationMinutes <= 0){
            log.error("Invalid Lockout duration configuration: {}. Must be positive.", lockoutDurationMinutes);
            throw new IllegalArgumentException("Lockout duration (minutes) must be positive");
        }

        if(resendDelaySeconds <= 0){
            log.error("Invalid Resend delay configuration: {}. Cannot be negative.", resendDelaySeconds);
            throw new IllegalArgumentException("Resend delay (seconds) cannot be negative");
        }

        if(maxResendRequestsPerHour <= 0){
            log.error("Invalid Max resend requests per hour configuration: {}. Must be positive.", maxResendRequestsPerHour);
            throw new IllegalArgumentException("Max resend requests per hour must be positive");
        }

        log.info("OTP Service configured: Length={}, Validity={}min, MaxAttempts={}, Lockout={}min, ResendDelay={}s, MaxResendPerHour={}",
                 otpLength, otpValidityMinutes, maxAttempts, lockoutDurationMinutes, resendDelaySeconds, maxResendRequestsPerHour);
    }

    public String generateAndStoreOtp(String identifier, String otpType){
        final String baseKey = otpType + " " + identifier;
        final String lockoutKey = OTP_LOCKOUT_PREFIX + baseKey;
        final String resendDelayKey = OTP_RESEND_DELAY_PREFIX + baseKey;
        final long currentHourTimestamp = Instant.now().getEpochSecond() / ONE_HOUR_IN_SECONDS;
        final String hourlyCountKey = OTP_RESEND_HOURLY_COUNT_PREFIX + baseKey + ":" + currentHourTimestamp;

        checkLockoutStatus(lockoutKey, otpType, identifier);

        checkResendDelay(resendDelayKey, otpType, identifier);

        checkAndIncrementHourlyLimit(hourlyCountKey, otpType, identifier);

        invalidateOtp(identifier, otpType);

        final String otp = generateOtpString();
        final String valueKey = OTP_VALUE_PREFIX + baseKey;
        final long otpValiditySeconds = otpValidityMinutes * ONE_MINUTE_IN_SECONDS;

        redisService.setValue(valueKey, otp, otpValiditySeconds);
        log.debug("Stored new OTP for {}:{} with validity {} seconds", otpType, identifier, otpValiditySeconds);

        resetAttemptCounter(baseKey, otpValiditySeconds);

        if(resendDelaySeconds > 0){
            redisService.setValue(resendDelayKey, "active", resendDelaySeconds);
            log.debug("Set resend delay for {}:{} for {} seconds", otpType, identifier, resendDelaySeconds);
        }

        log.info("Generated OTP successfully for {}:{}", otpType, identifier);

        return otp;

    }

    public boolean validateOtp(String identifier, String otpType, String otp){
        final String baseKey = otpType + ":" + identifier;
        final String valueKey = OTP_VALUE_PREFIX + baseKey;
        final String attemptKey = OTP_ATTEMPT_PREFIX + baseKey;
        final String lockoutKey = OTP_LOCKOUT_PREFIX + baseKey;

        checkLockoutStatus(lockoutKey, otpType, identifier);

        final String storedOtp = (String) redisService.getValue(valueKey);

        if(storedOtp == null){
            log.warn("OTP validation failed for {}:{} OTP not found or expired.", otpType, identifier);

            handleInvalidAttempt(identifier, otpType, attemptKey, lockoutKey);

            throw new OtpException("OTP is invalid or has expired. Please request a new one");
        }

        if(storedOtp.equals(otp)){
            handleSuccessfulValidation(identifier, otpType, valueKey, attemptKey);
            return true;
        } else {
            log.warn("OTP validation failed for {}:{} Incorrect OTP provided", otpType, identifier);
            handleInvalidAttempt(identifier, otpType, attemptKey, lockoutKey);

            return false;
        }
    }   

    public void invalidateOtp(String identifier, String otpType){
        final String valueKey = OTP_VALUE_PREFIX + otpType + ":" + identifier;
        redisService.deleteValue(valueKey);
        log.debug("Invalidated OTP for {}:{}", otpType, identifier);
    }

    private void checkLockoutStatus(String lockoutKey, String otpType, String identifier){
        if(redisService.getValue(lockoutKey) != null){
            long remainingLockoutSeconds = redisService.getTtl(lockoutKey);
            long remainingLockoutMinutes = (long) Math.ceil((double) remainingLockoutSeconds / ONE_MINUTE_IN_SECONDS);

            String message = String.format("Account is temporarily locked due to too mani failed attempts. Please try again in %d minute(s).", 
                                remainingLockoutMinutes > 0 ? remainingLockoutMinutes : 1);
            log.warn("Lockout active for {}:{}. Remaining: ~{} seconds", otpType, identifier, remainingLockoutSeconds);
            throw new OtpException(message);
        }
    }

    private void checkResendDelay(String resendDelayKey, String otpType, String identifier){
        if(resendDelaySeconds > 0 && redisService.getValue(resendDelayKey) != null){
            long remainingDelay  = redisService.getTtl(resendDelayKey);
            if(remainingDelay > 0){
                String message = String.format("Please wait %d second(s) before requesting a new code", remainingDelay );
                log.warn("Resend delay active for {}:{}. Remaining: {} seconds", otpType, identifier, remainingDelay);
                throw new OtpException(message);
            }
        }
    }

    private void checkAndIncrementHourlyLimit(String hourlyCountKey, String otpType, String identifier){
        Object currentCountObj = redisService.getValue(hourlyCountKey);
        long currentCount = (currentCountObj instanceof Number) ? ((Number) currentCountObj).longValue() : 0L;

        if(currentCount > maxResendRequestsPerHour){
            String message = String.format("You have reached the maximum limit of %d OTP requests per hour.", maxResendRequestsPerHour);
            log.warn("Hourly limit ({}/{}) reached for {}:{}", currentCount, maxResendRequestsPerHour, otpType, identifier);
            throw new OtpException(message);
        }

        long newCount  = redisService.getTtl(hourlyCountKey);

        if(newCount == 1 || redisService.getTtl(hourlyCountKey) < 0){
            redisService.expire(hourlyCountKey, ONE_HOUR_IN_SECONDS);
            log.debug("Set/Reset hourly count TTL for {}:{} for 1 hour", otpType, identifier);
        }

        log.debug("Hourly OTP request count for {}:{} is now {}/{}", otpType, identifier, newCount, maxResendRequestsPerHour);
    }

    private void resetAttemptCounter(String baseKey, long otpValiditySeconds){
        String attemptKey = OTP_ATTEMPT_PREFIX + baseKey;

        redisService.setValue(attemptKey, 0, otpValiditySeconds + 60);
        log.debug("Reset attempt counter for {}", baseKey);
    }

    private void handleSuccessfulValidation(String identifier, String otpType, String valueKey, String attemptKey){
        redisService.deleteValue(valueKey);

        redisService.deleteValue(attemptKey);
        log.info("Successful OTP validation for {}:{}. Cleaned up OTP and attempt keys.", otpType, identifier);
    }

    private void handleInvalidAttempt(String identifier, String otpType, String attemptKey, String lockoutKey){
        long attempts = redisService.incrementValue(attemptKey);

        long attemptKetTtl = redisService.getTtl(attemptKey);
        redisService.expire(attemptKey, attemptKetTtl);

        log.warn("Invalid OTP attempt {}/{} for {}:{}", attempts, maxAttempts, otpType, identifier);

        if(attempts >= maxAttempts){
            long lockoutSeconds = lockoutDurationMinutes * ONE_MINUTE_IN_SECONDS;
            redisService.setValue(lockoutKey, "looked", lockoutSeconds);

            redisService.deleteValue(attemptKey);

            String message = String.format("Too many failed attempts. Account is temporarily locked for %d minute(s).", lockoutDurationMinutes);
            
            log.error("Account locked for {}:{} for {} minutes due to {} failed attempts.",
                      otpType, identifier, lockoutDurationMinutes, attempts);
            throw new OtpException(message);
        }
    }

    private String generateOtpString(){
        StringBuilder otp = new StringBuilder(otpLength);
        for(int i = 0; i < otpLength; i++){
            otp.append(secureRandom.nextInt(10));
        }
        return otp.toString();
    }

}
