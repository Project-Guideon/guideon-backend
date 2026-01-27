package com.guideon.guideonbackend.domain.admin.entity;

import com.guideon.guideonbackend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "tb_admin",
    //데이터베이스 조회 성능 최적화
    indexes = {
        @Index(name = "idx_admin_email", columnList = "email"),
        @Index(name = "idx_admin_role_active", columnList = "role, is_active")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Admin extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_id")
    private Long adminId;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * 관리자 역할
     * - PLATFORM_ADMIN: 플랫폼 전체 관리자
     * - SITE_ADMIN: 관광지 운영자
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdminRole role;

    /**
     * 계정 활성화 상태
     * FALSE인 경우 로그인 및 모든 기능 차단
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;


    @Builder
    public Admin(String email, String passwordHash, AdminRole role) {
        this.email = email.toLowerCase(); // 이메일 소문자 정규화
        this.passwordHash = passwordHash;
        this.role = role;
        this.isActive = true;
    }
    //계정 비활성화
    public void deactivate() {
        this.isActive = false;
    }
    //계정 활성화
    public void activate() {
        this.isActive = true;
    }

    //마지막 로그인 시간 갱신
    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }
}
