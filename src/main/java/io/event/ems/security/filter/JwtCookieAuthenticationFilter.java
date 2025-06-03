package io.event.ems.security.filter;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.event.ems.security.jwt.JwtService;
import io.event.ems.service.impl.UserDetailsServiceImpl;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    private static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
    private static final String ERROR_JSON_FORMAT = "{\"error\":\"%s\", \"message\":\"%s\", \"status\":%d}";

    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // Extract JWT token from cookie
            String jwt = extractTokenFromCookie(request, ACCESS_TOKEN_COOKIE_NAME);

            // If no token found in cookie, continue with filter chain
            if (jwt == null || jwt.isBlank()) {
                log.warn("JWT token missing or empty");
                filterChain.doFilter(request, response);
                return;
            }

            String username = jwtService.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    setAuthenticationInContext(userDetails, request);
                    log.debug("JWT Cookie Authentication successful for user: {}", username);
                }
            }

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException ex) {
            log.error("JWT token in cookie is expired: {}", ex.getMessage());
            sendError(response, HttpStatus.UNAUTHORIZED, "Unauthorized", "JWT Token has expired");
        } catch (UnsupportedJwtException ex) {
            log.warn("JWT token in cookie is unsupported: {}", ex.getMessage());
            sendError(response, HttpStatus.UNAUTHORIZED, "Unauthorized", "Unsupported JWT token");
        } catch (MalformedJwtException ex) {
            log.warn("Invalid JWT token in cookie: {}", ex.getMessage());
            sendError(response, HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid JWT token format");
        } catch (SignatureException ex) {
            log.warn("Invalid JWT signature in cookie: {}", ex.getMessage());
            sendError(response, HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid JWT signature");
        } catch (IllegalArgumentException ex) {
            log.warn("JWT token compact of handler are invalid: {}", ex.getMessage());
            sendError(response, HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid JWT token");
        } catch (UsernameNotFoundException ex) {
            log.warn("User associated with JWT not found: {}", ex.getMessage());
            sendError(response, HttpStatus.UNAUTHORIZED, "Unauthorized", "User not found for the provided token");
        }
    }

    private String extractTokenFromCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void setAuthenticationInContext(UserDetails userDetails, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    private void sendError(HttpServletResponse response, HttpStatus status, String error, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(String.format(ERROR_JSON_FORMAT, error, message, status.value()));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/api/v1/auth/login") || path.equals("/api/v1/auth/register")
                || path.equals("/api/v1/auth/refresh-token");
    }
}