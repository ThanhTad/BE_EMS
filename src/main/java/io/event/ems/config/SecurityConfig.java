package io.event.ems.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import io.event.ems.security.filter.JwtCookieAuthenticationFilter;
import io.event.ems.service.impl.UserDetailsServiceImpl;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true) // Bật annotation bảo mật ở mức phương thức
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtCookieAuthenticationFilter jwtCookieAuthFilter;
        private final UserDetailsServiceImpl userDetailsService;

        // Các đường dẫn công khai không yêu cầu xác thực
        private static final String[] PUBLIC_PATHS = {
                        "/api/v1/auth/**",
                        "/v3/api-docs/**", // Cho OpenAPI v3
                        "/swagger-ui/**", // Cho Swagger UI
                        "/swagger-resources/**", // Các tài nguyên khác của Swagger
                        "/webjars/**" // Webjars thường dùng bởi Swagger UI
                        // Thêm các đường dẫn công khai khác nếu cần (ví dụ: trang chủ, trang giới
                        // thiệu...)
        };

        // Các đường dẫn chỉ dành cho ADMIN
        private static final String[] ADMIN_PATHS = {
                        "/api/v1/admin/**"
                        // Thêm các đường dẫn admin khác
        };

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                // 1. Cấu hình CSRF
                                // Khi sử dụng cookie, có thể cân nhắc bật CSRF protection
                                // Trong trường hợp JWT được lưu trong HttpOnly cookie, CSRF vẫn cần thiết
                                // Tuy nhiên, nếu API của bạn chỉ dùng cho các client không phải trình duyệt
                                // (như mobile app), có thể giữ CSRF disable
                                .csrf(AbstractHttpConfigurer::disable)

                                // 2. Cấu hình CORS (Cross-Origin Resource Sharing)
                                // Quan trọng: khi dùng cookie, cần đảm bảo allowCredentials(true)
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                                // 3. Phân quyền truy cập cho các request HTTP
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(PUBLIC_PATHS).permitAll()
                                                .requestMatchers(ADMIN_PATHS).hasRole("ADMIN")
                                                // Thay đổi này - những request khác cần xác thực
                                                .anyRequest().permitAll())

                                // 4. Quản lý session: STATELESS vì dùng JWT
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                // 5. Cấu hình Authentication Provider
                                .authenticationProvider(authenticationProvider())

                                // 6. Thêm bộ lọc JWT Cookie vào trước bộ lọc
                                // UsernamePasswordAuthenticationFilter
                                .addFilterBefore(jwtCookieAuthFilter, UsernamePasswordAuthenticationFilter.class)

                                // 7. Cấu hình các Security Headers để tăng cường bảo mật
                                .headers(headers -> headers
                                                // Content Security Policy (CSP): Giảm thiểu XSS
                                                .contentSecurityPolicy(csp -> csp
                                                                .policyDirectives(
                                                                                "default-src 'self'; " +
                                                                                                "script-src 'self'; " +
                                                                                                "style-src 'self' 'unsafe-inline'; "
                                                                                                +
                                                                                                "img-src 'self' data:; "
                                                                                                +
                                                                                                "font-src 'self'; " +
                                                                                                "connect-src 'self'; " +
                                                                                                "frame-src 'self'; " +
                                                                                                "object-src 'none'; " +
                                                                                                "form-action 'self'; " +
                                                                                                "base-uri 'self';"))
                                                // X-Content-Type-Options: Ngăn chặn tấn công MIME-sniffing
                                                .contentTypeOptions(contentTypeOptions -> {
                                                })

                                                // X-Frame-Options: Ngăn chặn clickjacking
                                                .frameOptions(frameOptions -> frameOptions.sameOrigin()));

                return http.build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public AuthenticationProvider authenticationProvider() {
                // Sử dụng constructor mới, truyền vào userDetailsService
                DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
                authProvider.setPasswordEncoder(passwordEncoder()); // còn set password encoder như cũ
                return authProvider;
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }

        @Bean
        public UrlBasedCorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();

                config.setAllowedOrigins(Arrays.asList(
                                "http://localhost:3000",
                                "http://localhost:8081",
                                "https://your-production-frontend.com"));

                config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

                config.setAllowedHeaders(Arrays.asList(
                                "Authorization",
                                "Content-Type",
                                "Accept",
                                "X-Requested-With",
                                "Origin",
                                "Access-Control-Request-Method",
                                "Access-Control-Request-Headers"));

                config.setExposedHeaders(Arrays.asList("Content-Disposition"));

                // Cực kỳ quan trọng khi sử dụng cookie
                config.setAllowCredentials(true);

                config.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return source;
        }
}