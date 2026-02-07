package com.guideon.core.dto;

import com.guideon.core.domain.zone.entity.Zone;
import com.guideon.core.global.util.GeoJsonUtil;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Core 내부 통신용 Zone DTO
 */
@Getter
@Builder
public class ZoneDto {

    private Long zoneId;
    private Long siteId;
    private String name;
    private String code;
    private String zoneType;
    private Integer level;
    private Long parentZoneId;
    private Map<String, Object> areaGeojson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ZoneDto from(Zone zone) {
        return ZoneDto.builder()
                .zoneId(zone.getZoneId())
                .siteId(zone.getSite().getSiteId())
                .name(zone.getName())
                .code(zone.getCode())
                .zoneType(zone.getZoneType().name())
                .level(zone.getLevel())
                .parentZoneId(zone.getParentZone() != null ? zone.getParentZone().getZoneId() : null)
                .areaGeojson(GeoJsonUtil.fromGeometry(zone.getAreaGeometry()))
                .createdAt(zone.getCreatedAt())
                .updatedAt(zone.getUpdatedAt())
                .build();
    }
}
