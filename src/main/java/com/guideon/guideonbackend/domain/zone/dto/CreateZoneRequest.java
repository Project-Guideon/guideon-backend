package com.guideon.guideonbackend.domain.zone.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
public class CreateZoneRequest {

    @Schema(description = "구역 표시명", example = "근정전 권역")
    @NotBlank(message = "구역 이름은 필수입니다")
    @Size(max = 50, message = "구역 이름은 50자 이하여야 합니다")
    private String name;

    @Schema(description = "site 내 유니크 코드", example = "SUB_A")
    @NotBlank(message = "구역 코드는 필수입니다")
    @Size(max = 50, message = "구역 코드는 50자 이하여야 합니다")
    private String code;

    @Schema(description = "INNER 또는 SUB", example = "SUB")
    @NotNull(message = "구역 타입은 필수입니다")
    @JsonProperty("zone_type")
    private String zoneType;

    @Schema(description = "SUB면 필수, INNER면 null", example = "1")
    @JsonProperty("parent_zone_id")
    private Long parentZoneId;

    @Schema(description = "GeoJSON Polygon", implementation = GeoJsonPolygonSchema.class)
    @NotNull(message = "영역 GeoJSON은 필수입니다")
    @JsonProperty("area_geojson")
    private Map<String, Object> areaGeojson;
}
