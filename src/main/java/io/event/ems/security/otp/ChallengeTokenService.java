package io.event.ems.security.otp;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChallengeTokenService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String CHALLENGE_TOKEN_PREFIX = "challenge_token:";
    private static final long EXPIRATION_TIME_SECONDS = 300; // 5 minutes

    public String generateChallengerToken(String identifier) {
        String token = java.util.UUID.randomUUID().toString();
        String key = CHALLENGE_TOKEN_PREFIX + identifier;

        redisTemplate.opsForValue().set(key, token, EXPIRATION_TIME_SECONDS, java.util.concurrent.TimeUnit.SECONDS);

        log.debug("Generated challenge token for {}", identifier);
        return token;
    }

    public boolean validateChallengeToken(String identifier, String token) {
        String key = CHALLENGE_TOKEN_PREFIX + identifier;
        String storedToken = redisTemplate.opsForValue().get(key);

        if (storedToken != null && storedToken.equals(token)) {
            redisTemplate.delete(key); // Invalidate the token after successful validation
            log.debug("Validated challenge token for {}", identifier);
            return true;
        }

        log.warn("Invalid challenge token for {}", identifier);
        return false;
    }

    public void invalidateChallengeToken(String identifier) {
        String key = CHALLENGE_TOKEN_PREFIX + identifier;
        redisTemplate.delete(key);
        log.debug("Invalidated challenge token for {}", identifier);
    }

}
