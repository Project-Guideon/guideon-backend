package com.guideon.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 키오스크 부트스트랩 응답 DTO
 * 기기 초기 설정에 필요한 site/zone/mascot 컨텍스트 포함
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KioskBootstrapDto {

    private String deviceId;
    private Long siteId;
    private String siteName;

    private Long zoneId;       // nullable (OUTER zone)
    private String zoneName;   // nullable

    private MascotSummary mascot;  // nullable (마스코트 미등록 site 허용)

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MascotSummary {
        private String name;
        private String modelId;
        private String defaultAnim;
        private String greetingMsg;
        private String ttsVoiceId;
        private Map<String, Object> ttsVoiceJson;
    }
}