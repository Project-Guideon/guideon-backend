package com.guideon.core.dto.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * FastAPI /internal/v1/qa 요청 DTO
 *
 * Core가 DB에서 context(nearbyPlaces, dailyInfos)를 조립하여 FastAPI에 전달.
 * FastAPI는 PDF만 RAG 검색하고, Place/DailyInfo는 이 context를 활용.
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
        private List<NearbyPlace> nearbyPlaces;
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

    /**
     * Core가 PostGIS 공간 검색으로 조립한 근처 장소 정보.
     * FastAPI LangGraph 위치 안내 라우트에서 활용.
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NearbyPlace {
        private Long placeId;
        private String name;
        private String category;
        private String description;
        private Double distanceM;
        private boolean sameZone;
    }
}
