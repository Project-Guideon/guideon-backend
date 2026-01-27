package com.guideon.guideonbackend.domain.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * 관리자 로그인 응답 DTO
 */
@Getter
@Builder
public class AdminLoginResponse {

    //Access Token (15분 유효)
    @JsonProperty("access_token")
    private String accessToken;

    //Refresh Token (30일 유효)
    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("admin_id")
    private Long adminId;

    private String email;

    //관리자 역할 (PLATFORM_ADMIN, SITE_ADMIN)
    private String role;
}
