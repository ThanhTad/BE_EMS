package io.event.ems.security.jwt;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.event.ems.exception.UnauthorizedException;
import io.event.ems.service.RedisService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token.expiration-ms}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token.expiration-ms}")
    private long refreshTokenExpiration;

    @Value("${redis.ttl}")
    private long redisTtl;

    private final RedisService redisService;

    private static final String REFRESH_TOKEN_BLACKLIST_PREFEX = "blacklist:refresh";

    public String generateAccessToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails, accessTokenExpiration, null);
    }

    public String generateRefreshToken(UserDetails userDetails, UUID userId) {
        String jti = UUID.randomUUID().toString();
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", userId.toString());
        extraClaims.put("type", "refresh_token");

        return generateToken(extraClaims, userDetails, accessTokenExpiration, jti);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration, String jti) {
        return null;
    }

    public String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration, String jti) {
        JwtBuilder builder = Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey());

        if (jti != null) {
            builder.id(jti);
        }

        return builder.compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            boolean valid = (username.equals(userDetails.getUsername())) && !isTokenExpired(token);

            if (valid && isRefreshToken(token)) {
                String jti = extractJti(token);
                if (jti != null && isRefreshTokenBlacklisted(jti)) {
                    log.warn("Attempted to use blacklisted refresh token (JTI: {})", jti);
                    return false;
                }
            }

            return valid;

        } catch (SecurityException | MalformedJwtException e) {
            log.error("Invalid JWT: {}", e.getMessage());
            return false;
        } catch (ExpiredJwtException e) {
            return false;
        } catch (UnsupportedJwtException | IllegalArgumentException e) {
            log.error("JWT error: {}", e.getMessage());
            return false;
        }
    }

    public void blacklistRefreshToken(String jti) {
        if (jti == null)
            return;
        String key = REFRESH_TOKEN_BLACKLIST_PREFEX + jti;
        redisService.setValue(key, "blacklisted", redisTtl);
        log.info("Blacklistedl refresh token (JTI: {})", jti);
    }

    public boolean isRefreshTokenBlacklisted(String jti) {
        if (jti == null)
            return false;
        String key = REFRESH_TOKEN_BLACKLIST_PREFEX + jti;
        return redisService.getValue(key) != null;
    }

    public String extractJti(String token) {
        try {

            Claims claims = extractAllClaims(token);
            return claims.getId();

        } catch (JwtException | IllegalArgumentException e) {
            log.error("Failed to extract Jti: {}", e.getMessage());
            return null;
        }
    }

    public boolean isRefreshToken(String token) {
        try {

            Claims claims = extractAllClaims(token);
            String type = claims.get("type", String.class);
            return "refresh_token".equals(type);

        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Error checking token expiration: {}", e.getMessage());
            return false;
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public boolean isValidToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid token: {}", e.getMessage());
            return false;
        }
    }

    public String extractValidatedToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.warn("Authorization header missing or invalid");
            throw new IllegalArgumentException("Missing or Invalid Authorization Header");
        }

        String token = extractTokenFromHeader(authorizationHeader);

        if (!isValidToken(token)) {
            log.warn("Invalid or expired access token");
            throw new UnauthorizedException("Invalid or expired access token");
        }

        return token;
    }

    public String extractTokenFromHeader(String authorizationHeader) {
        return authorizationHeader.substring(7);
    }

}
