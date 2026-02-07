package com.guideon.guideonbackend.domain.zone.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.guideon.core.dto.ZoneDto;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
public class ZoneResponse {

    @JsonProperty("zone_id")
    private Long zoneId;

    @JsonProperty("site_id")
    private Long siteId;

    private String name;

    private String code;

    @JsonProperty("zone_type")
    private String zoneType;

    private Integer level;

    @JsonProperty("parent_zone_id")
    private Long parentZoneId;

    @JsonProperty("area_geojson")
    private Map<String, Object> areaGeojson;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    public static ZoneResponse from(ZoneDto dto) {
        return ZoneResponse.builder()
                .zoneId(dto.getZoneId())
                .siteId(dto.getSiteId())
                .name(dto.getName())
                .code(dto.getCode())
                .zoneType(dto.getZoneType())
                .level(dto.getLevel())
                .parentZoneId(dto.getParentZoneId())
                .areaGeojson(dto.getAreaGeojson())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }
}
