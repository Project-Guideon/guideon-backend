package com.guideon.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Zone 생성 요청 Command
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateZoneCommand {
    private String name;
    private String code;
    private String zoneType;
    private Long parentZoneId;
    private Map<String, Object> areaGeojson;
}
