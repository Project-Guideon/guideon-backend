package com.guideon.core.dto.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * FastAPI /internal/v1/qa 요청 DTO
 * Kiosk BFF가 context를 조립하여 FastAPI에 전달
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QaRequest {

    private String sessionId;
    private Long siteId;
    private String question;
    private String language;

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
