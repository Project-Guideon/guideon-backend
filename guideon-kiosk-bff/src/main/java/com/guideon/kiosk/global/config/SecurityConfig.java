package com.guideon.kiosk.global.config;

import com.guideon.core.domain.device.repository.DeviceRepository;
import com.guideon.kiosk.global.security.DeviceAuthEntryPoint;
import com.guideon.kiosk.global.security.DeviceTokenAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Kiosk BFF Security 설정
 *
 * JWT 없이 Device Token(X-DEVICE-TOKEN) 기반 인증.
 * 모든 /api/v1/kiosk/** 엔드포인트는 DEVICE 역할 필요.
 * 단, /api/v1/kiosk/pairing/**는 토큰 없는 상태에서 호출되므로 permitAll.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final DeviceAuthEntryPoint deviceAuthEntryPoint;
    private final DeviceRepository deviceRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(deviceAuthEntryPoint)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/health",
                                "/error",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/ws/**",  // WS 핸드셰이크는 인터셉터에서 직접 인증
                                "/api/v1/kiosk/pairing/**"  // 페어링은 토큰 없는 상태에서 호출
                        ).permitAll()
                        .requestMatchers("/api/v1/kiosk/**").hasRole("DEVICE")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        new DeviceTokenAuthFilter(deviceRepository),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("X-Trace-Id"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
