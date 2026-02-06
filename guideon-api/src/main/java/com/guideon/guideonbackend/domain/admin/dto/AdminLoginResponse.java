package com.guideon.guideonbackend.domain.admin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 관리자 로그인 응답 DTO
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
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

    // SITE_ADMIN인 경우 접근 가능 사이트 ID 목록
    @JsonProperty("site_ids")
    private List<Long> siteIds;
}
