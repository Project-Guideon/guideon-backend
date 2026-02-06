package com.guideon.core.domain.admin.entity;

import com.guideon.core.domain.site.entity.Site;
import com.guideon.core.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "tb_admin_invite",
    indexes = {
        @Index(name = "idx_invite_site_status", columnList = "site_id, status"),
        @Index(name = "idx_invite_expires_at", columnList = "expires_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminInvite extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invite_id")
    private Long inviteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdminRole role;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdminInviteStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_by_admin_id")
    private Long createdByAdminId;

    @Builder
    public AdminInvite(Site site, String email, AdminRole role,
                       String tokenHash, LocalDateTime expiresAt,
                       Long createdByAdminId) {
        this.site = site;
        this.email = email.toLowerCase();
        this.role = role;
        this.tokenHash = tokenHash;
        this.status = AdminInviteStatus.PENDING;
        this.expiresAt = expiresAt;
        this.createdByAdminId = createdByAdminId;
    }

    public void markUsed() {
        this.status = AdminInviteStatus.USED;
        this.usedAt = LocalDateTime.now();
    }

    public void markExpired() {
        this.status = AdminInviteStatus.EXPIRED;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}
