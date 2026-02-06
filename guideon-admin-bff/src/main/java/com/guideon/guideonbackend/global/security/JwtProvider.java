package com.guideon.guideonbackend.global.security;

import com.guideon.core.domain.admin.entity.AdminRole;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * JWT 토큰 생성 및 검증 Provider
 */
@Slf4j
@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;

    /**
     * @param secret JWT 서명에 사용할 비밀키
     * @param accessTokenValidityMs Access Token 유효 기간
     * @param refreshTokenValidityMs Refresh Token 유효 기간
     */
    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity-ms}") long accessTokenValidityMs,
            @Value("${jwt.refresh-token-validity-ms}") long refreshTokenValidityMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityMs = accessTokenValidityMs;
        this.refreshTokenValidityMs = refreshTokenValidityMs;
    }

    /**
     * Access Token 생성
     *
     * @param adminId 관리자 ID
     * @param email 관리자 이메일
     * @param role 관리자 역할
     * @return JWT Access Token
     */
    public String createAccessToken(Long adminId, String email, AdminRole role) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenValidityMs);

        return Jwts.builder()
                .setSubject(adminId.toString())
                .claim("email", email)
                .claim("role", role.name())
                .claim("type", "access")
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Refresh Token 생성
     *
     * @param adminId 관리자 ID
     * @return JWT Refresh Token
     */
    public String createRefreshToken(Long adminId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshTokenValidityMs);

        return Jwts.builder()
                .setSubject(adminId.toString())
                .claim("type", "refresh")
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // Refresh Token 만료 시간 계산
    public LocalDateTime calculateRefreshTokenExpiry() {
        return LocalDateTime.now()
                .plusSeconds(refreshTokenValidityMs / 1000);
    }

    /**
     * 토큰 검증 및 Claims 추출
     *
     * @param token JWT 토큰
     * @return Claims
     * @throws ExpiredJwtException 토큰 만료
     * @throws JwtException 유효하지 않은 토큰
     */
    public Claims validateAndGetClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.debug("Token expired: {}", e.getMessage());
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid token: {}", e.getMessage());
            throw e;
        }
    }

    //토큰에서 관리자 ID 추출
    public Long getAdminId(String token) {
        Claims claims = validateAndGetClaims(token);
        return Long.parseLong(claims.getSubject());
    }


    //토큰 타입 확인 (access/refresh)
    public String getTokenType(String token) {
        Claims claims = validateAndGetClaims(token);
        return claims.get("type", String.class);
    }
}
