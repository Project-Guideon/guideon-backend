package com.guideon.kiosk.global.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 인증된 디바이스(키오스크) 정보
 *
 * Device Token 검증 후 SecurityContext에 저장되어 @AuthenticationPrincipal로 접근.
 */
@Getter
@AllArgsConstructor
public class DeviceDetails {
    private String deviceId;
    private Long siteId;
    private Long zoneId;      // nullable (OUTER zone)
    private Double latitude;  // 디바이스 설치 좌표
    private Double longitude;
}