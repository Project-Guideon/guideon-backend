package com.guideon.guideonbackend.domain.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토큰 갱신 요청 DTO
 */
@Getter
@NoArgsConstructor
public class AdminRefreshRequest {

    //Refresh Token
    @NotBlank(message = "Refresh Token은 필수입니다")
    @JsonProperty("refresh_token")
    private String refreshToken;
}
