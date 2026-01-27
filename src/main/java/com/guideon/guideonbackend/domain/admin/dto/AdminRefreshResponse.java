package com.guideon.guideonbackend.domain.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * 토큰 갱신 응답 DTO
 */
@Getter
@Builder
public class AdminRefreshResponse {

    //새로운 Access Token
    @JsonProperty("access_token")
    private String accessToken;

    //새로운 Refresh Token
    @JsonProperty("refresh_token")
    private String refreshToken;
}
