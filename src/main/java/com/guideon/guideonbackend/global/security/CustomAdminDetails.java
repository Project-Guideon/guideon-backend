package com.guideon.guideonbackend.global.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 인증된 관리자 정보 DTO
 *
 * JWT 토큰에서 추출한 관리자 정보를 담음.
 * SecurityContext에 저장되어 @AuthenticationPrincipal로 접근.
 */
@Getter
@AllArgsConstructor
public class CustomAdminDetails {
    private Long adminId;
    private String email;
    private String role;
}
