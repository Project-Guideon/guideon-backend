package com.guideon.core.dto;

import com.guideon.core.domain.admin.entity.AdminInvite;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Core 내부 통신용 Invite DTO
 */
@Getter
@Builder
public class InviteDto {

    private Long inviteId;
    private Long siteId;
    private String siteName;
    private String email;
    private String role;
    private String status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private String rawToken; // 생성 시에만 포함 (1회 노출)

    public static InviteDto from(AdminInvite invite) {
        return InviteDto.builder()
                .inviteId(invite.getInviteId())
                .siteId(invite.getSite().getSiteId())
                .siteName(invite.getSite().getName())
                .email(invite.getEmail())
                .role(invite.getRole().name())
                .status(invite.getStatus().name())
                .expiresAt(invite.getExpiresAt())
                .createdAt(invite.getCreatedAt())
                .build();
    }

    public static InviteDto fromWithToken(AdminInvite invite, String rawToken) {
        return InviteDto.builder()
                .inviteId(invite.getInviteId())
                .siteId(invite.getSite().getSiteId())
                .siteName(invite.getSite().getName())
                .email(invite.getEmail())
                .role(invite.getRole().name())
                .status(invite.getStatus().name())
                .expiresAt(invite.getExpiresAt())
                .createdAt(invite.getCreatedAt())
                .rawToken(rawToken)
                .build();
    }
}
