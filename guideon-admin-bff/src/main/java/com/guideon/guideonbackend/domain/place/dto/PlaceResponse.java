package com.guideon.guideonbackend.domain.place.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.guideon.core.dto.PlaceDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
@Schema(description = "장소 응답")
public class PlaceResponse {

    @JsonProperty("place_id")
    @Schema(description = "장소 ID", example = "100")
    private Long placeId;

    @JsonProperty("site_id")
    @Schema(description = "관광지 ID", example = "1")
    private Long siteId;

    @JsonProperty("zone_id")
    @Schema(description = "구역 ID (OUTER면 null)", example = "12")
    private Long zoneId;

    @JsonProperty("zone_source")
    @Schema(description = "구역 할당 방식", example = "AUTO")
    private String zoneSource;

    @Schema(description = "기본 장소명", example = "근정전 화장실")
    private String name;

    @JsonProperty("name_json")
    @Schema(description = "다국어 장소명")
    private Map<String, String> nameJson;

    @Schema(description = "카테고리", example = "TOILET")
    private String category;

    @Schema(description = "위도", example = "37.5796")
    private Double latitude;

    @Schema(description = "경도", example = "126.977")
    private Double longitude;

    @Schema(description = "설명", example = "근정전 우측 50m")
    private String description;

    @JsonProperty("image_url")
    @Schema(description = "썸네일 URL")
    private String imageUrl;

    @JsonProperty("is_active")
    @Schema(description = "활성화 여부", example = "true")
    private Boolean isActive;

    @JsonProperty("created_at")
    @Schema(description = "생성 시각")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    @Schema(description = "수정 시각")
    private LocalDateTime updatedAt;

    public static PlaceResponse from(PlaceDto dto) {
        return PlaceResponse.builder()
                .placeId(dto.getPlaceId())
                .siteId(dto.getSiteId())
                .zoneId(dto.getZoneId())
                .zoneSource(dto.getZoneSource())
                .name(dto.getName())
                .nameJson(dto.getNameJson())
                .category(dto.getCategory())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .description(dto.getDescription())
                .imageUrl(dto.getImageUrl())
                .isActive(dto.getIsActive())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }
}
