package com.guideon.guideonbackend.global.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 모든 HTTP 요청에서 Authorization 헤더를 확인하고 JWT를 검증
 */

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String token = extractToken(request);

            if (token != null) {
                // 토큰 검증 및 파싱
                Claims claims = jwtProvider.validateAndGetClaims(token);

                // Access Token인지 확인
                String tokenType = claims.get("type", String.class);
                if (!"access".equals(tokenType)) {
                    log.debug("Not an access token");
                    filterChain.doFilter(request, response);
                    return;
                }

                // 관리자 정보 추출
                Long adminId = Long.parseLong(claims.getSubject());
                String email = claims.get("email", String.class);
                String role = claims.get("role", String.class);

                // 인증 객체 생성
                CustomAdminDetails adminDetails = new CustomAdminDetails(adminId, email, role);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                adminDetails,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // SecurityContext에 인증 정보 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            log.debug("JWT authentication failed: {}", e.getMessage());
            // 인증 실패 시 필터 체인 계속 (JwtAuthenticationEntryPoint에서 처리)
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
