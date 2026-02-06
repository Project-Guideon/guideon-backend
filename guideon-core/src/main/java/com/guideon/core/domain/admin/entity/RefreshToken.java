package com.guideon.core.domain.admin.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.time.LocalDateTime;

/**
 * Redis에 저장되며 TTL(30일)로 자동 만료 관리
 * 토큰 재사용 방지를 위해 로그인/갱신/로그아웃 시 기존 토큰을 삭제하고 새로 발급
 */
@RedisHash(value = "refreshToken", timeToLive = 2592000) // 30일 (초 단위)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    private Long adminId;

    //Refresh Token 문자열
    private String token;

    //토큰 만료 시간
    private LocalDateTime expiresAt;

    //토큰 생성 시간
    private LocalDateTime createdAt;

    @Builder
    public RefreshToken(String token, Long adminId, LocalDateTime expiresAt) {
        this.token = token;
        this.adminId = adminId;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 토큰 만료 여부 확인
     *
     * @return 만료되었으면 true, 아니면 false
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
