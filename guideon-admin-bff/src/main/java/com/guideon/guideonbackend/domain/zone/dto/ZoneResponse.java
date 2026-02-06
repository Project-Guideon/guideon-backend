package com.guideon.guideonbackend.domain.zone.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.guideon.core.domain.zone.entity.Zone;
import com.guideon.core.global.util.GeoJsonUtil;
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

    private Short level;

    @JsonProperty("parent_zone_id")
    private Long parentZoneId;

    @JsonProperty("area_geojson")
    private Map<String, Object> areaGeojson;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    public static ZoneResponse from(Zone zone) {
        return ZoneResponse.builder()
                .zoneId(zone.getZoneId())
                .siteId(zone.getSite().getSiteId())
                .name(zone.getName())
                .code(zone.getCode())
                .zoneType(zone.getZoneType().name())
                .level(zone.getLevel())
                .parentZoneId(zone.getParentZone() != null ? zone.getParentZone().getZoneId() : null)
                .areaGeojson(GeoJsonUtil.toGeoJson(zone.getAreaGeometry()))
                .createdAt(zone.getCreatedAt())
                .updatedAt(zone.getUpdatedAt())
                .build();
    }
}
