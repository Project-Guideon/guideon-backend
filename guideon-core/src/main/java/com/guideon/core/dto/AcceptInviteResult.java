package com.guideon.core.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 초대 수락 결과 (계정 생성 완료 후 반환)
 * BFF에서 JWT 발급에 필요한 정보 포함
 */
@Getter
@Builder
public class AcceptInviteResult {
    private Long adminId;
    private String email;
    private String role;
    private List<Long> siteIds;
}
