package com.guideon.core.dto.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * FastAPI /internal/v1/qa 요청 DTO
 *
 * - nearbyPlaces: FastAPI fetch_places_node 가 category 추출 후 직접 Spring Boot 에 조회 요청
 * - systemPrompt: tb_mascot 에서 조회한 마스코트 캐릭터 프롬프트
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QaRequest {

    private String sessionId;
    private Long siteId;
    private String deviceId;
    private String question;
    private String language;
    private String systemPrompt;

    private DeviceLocation deviceLocation;

    private QaContext context;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceLocation {
        private Double latitude;
        private Double longitude;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QaContext {
        private List<DailyInfoSummary> dailyInfos;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyInfoSummary {
        private String placeName;
        private String infoType;
        private String content;
    }

}
