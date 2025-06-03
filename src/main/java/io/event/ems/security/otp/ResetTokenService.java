package io.event.ems.security.otp;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResetTokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String RESET_TOKEN_PREFIX = "reset_token:";

    public void storeResetToken(String email, String resetToken, Duration expiration) {
        String key = RESET_TOKEN_PREFIX + email;
        redisTemplate.opsForValue().set(key, resetToken, expiration);
    }

    public boolean validateResetToken(String email, String resetToken) {
        String key = RESET_TOKEN_PREFIX + email;
        String storedToken = redisTemplate.opsForValue().get(key);
        return storedToken != null && storedToken.equals(resetToken);
    }

    public void invalidateResetToken(String email) {
        String key = RESET_TOKEN_PREFIX + email;
        redisTemplate.delete(key);
    }

}
