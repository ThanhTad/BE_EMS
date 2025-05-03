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

import io.event.ems.security.filter.JwtAuthenticationFilter;
import io.event.ems.service.impl.UserDetailsServiceImpl;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true) // Bật annotation bảo mật ở mức phương thức
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
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
                // 1. Vô hiệu hóa CSRF (vì dùng JWT - stateless)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Cấu hình CORS (Cross-Origin Resource Sharing)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 3. Phân quyền truy cập cho các request HTTP
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .requestMatchers(ADMIN_PATHS).hasRole("ADMIN")

                        .anyRequest().permitAll())

                // 4. Quản lý session: STATELESS vì dùng JWT
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 5. Cấu hình Authentication Provider
                .authenticationProvider(authenticationProvider())

                // 7. Thêm bộ lọc JWT vào trước bộ lọc UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                // 8. Cấu hình các Security Headers để tăng cường bảo mật
                .headers(headers -> headers
                        // Content Security Policy (CSP): Giảm thiểu XSS
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "default-src 'self'; " + // Nguồn mặc định: chỉ từ domain hiện tại
                                                "script-src 'self'; " + // Chỉ cho phép script từ domain hiện tại (Loại
                                                                        // bỏ 'unsafe-inline' nếu có thể!)
                                                "style-src 'self' 'unsafe-inline'; " + // Cho phép style từ domain hiện
                                                                                       // tại và inline style (Cân nhắc
                                                                                       // loại bỏ 'unsafe-inline')
                                                "img-src 'self' data:; " + // Cho phép ảnh từ domain hiện tại và dạng
                                                                           // data URI
                                                "font-src 'self'; " + // Cho phép font từ domain hiện tại
                                                "connect-src 'self'; " + // Cho phép kết nối (API call) đến domain hiện
                                                                         // tại
                                                "frame-src 'self'; " + // Cho phép nhúng frame từ domain hiện tại
                                                "object-src 'none'; " + // Không cho phép các plugin như Flash
                                                "form-action 'self'; " + // Chỉ cho phép form submit đến domain hiện tại
                                                "base-uri 'self';" // Hạn chế thẻ <base>
                                ))
                        // HTTP Strict Transport Security (HSTS): Bắt buộc HTTPS
                        // .httpStrictTransportSecurity(hsts -> hsts
                        // .includeSubDomains(true) // Áp dụng cho cả subdomain
                        // .maxAgeInSeconds(31536000) // Thời gian hiệu lực 1 năm
                        // )
                        // X-Content-Type-Options: Ngăn chặn tấn công MIME-sniffing
                        .contentTypeOptions(contentTypeOptions -> contentTypeOptions.disable())

                        // X-Frame-Options: Ngăn chặn clickjacking
                        .frameOptions(frameOptions -> frameOptions.sameOrigin()) // Chỉ cho phép nhúng frame từ cùng
                                                                                 // domain
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
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

        config.setAllowCredentials(true);

        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}