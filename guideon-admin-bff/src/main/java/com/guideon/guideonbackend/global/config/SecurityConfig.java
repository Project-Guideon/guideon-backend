package com.guideon.guideonbackend.global.config;

import com.guideon.guideonbackend.global.security.JwtAuthenticationEntryPoint;
import com.guideon.guideonbackend.global.security.JwtAuthenticationFilter;
import com.guideon.guideonbackend.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 설정
 *
 * JWT 기반 인증, CORS, CSRF, 세션 정책 등을 설정합니다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (JWT 사용)
                .csrf(AbstractHttpConfigurer::disable)

                // CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 세션 사용 안 함 (Stateless)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 예외 처리
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )

                // 경로별 인증 설정
                .authorizeHttpRequests(auth -> auth
                        // 공개 엔드포인트
                        .requestMatchers(
                                "/api/v1/admin/auth/login",
                                "/api/v1/admin/auth/refresh",
                                "/health",
                                "/error",
                                // Swagger UI
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**"
                        ).permitAll()
                        // 초대 수락은 비인증 접근 허용
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/invites/*/accept").permitAll()
                        // 초대 관리 API는 PLATFORM_ADMIN만 접근 가능
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/invites").hasRole("PLATFORM_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/invites").hasRole("PLATFORM_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/invites/*/expire").hasRole("PLATFORM_ADMIN")
                        // Zone 관리 API는 PLATFORM_ADMIN + SITE_ADMIN 접근 가능
                        .requestMatchers("/api/v1/admin/sites/*/zones/**").hasAnyRole("PLATFORM_ADMIN", "SITE_ADMIN")
                        // Place 관리 API는 PLATFORM_ADMIN + SITE_ADMIN 접근 가능
                        .requestMatchers("/api/v1/admin/sites/*/places/**").hasAnyRole("PLATFORM_ADMIN", "SITE_ADMIN")
                        // Site 관리 API는 PLATFORM_ADMIN만 접근 가능
                        .requestMatchers("/api/v1/admin/sites/**").hasRole("PLATFORM_ADMIN")
                        .anyRequest().authenticated()
                )

                // JWT 필터 추가 (UsernamePasswordAuthenticationFilter 전에 실행)
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtProvider),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    /**
     * CORS 설정
     *
     * @return CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "X-Trace-Id"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * 비밀번호 암호화 Encoder
     *
     * @return BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
