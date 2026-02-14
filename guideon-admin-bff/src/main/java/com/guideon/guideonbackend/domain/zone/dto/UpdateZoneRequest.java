package com.guideon.guideonbackend.domain.zone.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
public class UpdateZoneRequest {

    @Schema(description = "구역 표시명", example = "수정된 권역명")
    @Size(max = 50, message = "구역 이름은 50자 이하여야 합니다")
    private String name;

    @Schema(description = "site 내 유니크 코드", example = "SUB_B")
    @Size(max = 50, message = "구역 코드는 50자 이하여야 합니다")
    private String code;

    @Schema(description = "GeoJSON Polygon", implementation = GeoJsonPolygonSchema.class)
    @JsonProperty("area_geojson")
    private Map<String, Object> areaGeojson;
}