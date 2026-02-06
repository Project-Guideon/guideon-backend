package com.guideon.guideonbackend.domain.admin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InviteResponse {

    @JsonProperty("invite_id")
    private Long inviteId;

    @JsonProperty("site_id")
    private Long siteId;

    @JsonProperty("site_name")
    private String siteName;

    private String email;

    private String role;

    private String status;

    @JsonProperty("expires_at")
    private LocalDateTime expiresAt;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    // 초대 생성 시에만 1회 반환, 이후 조회 불가
    private String token;
}
