package com.fashionstore.backend.configuration;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SecurityConfig {

    CustomJwtDecoder customJwtDecoder;
    CorsConfig corsConfig;
    JwtAuthenticationConfig jwtAuthenticationConfig;
    JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    ApiAccessDeniedHandler apiAccessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        // Phân quyền truy cập các API.
        httpSecurity.authorizeHttpRequests(request -> request
                .requestMatchers(
                        HttpMethod.POST,
                        "/auth/login",
                        "/auth/refresh_token",
                        "/api/auth/login",
                        "/api/auth/refresh_token")
                .permitAll()
                .requestMatchers(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**")
                .permitAll()
                .anyRequest()
                .authenticated());

        // Xác thực access token JWT và chuyển claim thành quyền Spring Security.
        httpSecurity.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                        .decoder(customJwtDecoder)
                        .jwtAuthenticationConverter(
                                jwtAuthenticationConfig.jwtAuthenticationConverter()))
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(apiAccessDeniedHandler));

        // API dùng bearer token, không tạo session đăng nhập.
        httpSecurity.sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // CORS lấy cấu hình từ CorsConfig.
        httpSecurity.cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()));

        // Tắt CSRF vì API dùng JWT.
        httpSecurity.csrf(AbstractHttpConfigurer::disable);

        return httpSecurity.build();
    }
}
