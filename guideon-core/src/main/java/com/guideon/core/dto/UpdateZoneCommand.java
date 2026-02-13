package com.guideon.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Zone 수정 요청 Command
 * zone_type, parent_zone_id는 구조적 제약으로 변경 불가
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateZoneCommand {
    private String name;
    private String code;
    private Map<String, Object> areaGeojson;
}