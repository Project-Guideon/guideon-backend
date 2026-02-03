package com.guideon.guideonbackend.domain.zone.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Swagger 스키마 표시 전용 클래스
 * 실제 직렬화/역직렬화에는 사용되지 않음
 */
@Schema(description = "GeoJSON Polygon")
public class GeoJsonPolygonSchema {

    @Schema(description = "GeoJSON 타입", example = "Polygon")
    public String type;

    @Schema(description = "폴리곤 좌표 배열 [[[lng, lat], ...]],", example = "[[126.9765, 37.5791]]")
    public List<List<List<Double>>> coordinates;
}
