package com.guideon.guideonbackend.domain.place.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "장소 수정 요청")
public class UpdatePlaceRequest {

    @Schema(description = "장소명", example = "근정전 매점")
    @Size(max = 100, message = "장소 이름은 100자 이하여야 합니다")
    private String name;

    @Schema(description = "카테고리", example = "SHOP")
    @Size(max = 50, message = "카테고리는 50자 이하여야 합니다")
    private String category;

    @Schema(description = "설명", example = "설명 변경")
    private String description;

    @Schema(description = "썸네일 URL", example = "https://.../shop.png")
    @JsonProperty("image_url")
    @Size(max = 500, message = "이미지 URL은 500자 이하여야 합니다")
    private String imageUrl;

    @Schema(description = "위도", example = "37.5796")
    private Double latitude;

    @Schema(description = "경도", example = "126.977")
    private Double longitude;

    @Schema(description = "활성화 여부", example = "true")
    @JsonProperty("is_active")
    private Boolean isActive;

    @Schema(description = "구역 할당 방식 (AUTO/MANUAL)", example = "MANUAL")
    @JsonProperty("zone_source")
    private String zoneSource;

    @Schema(description = "구역 ID (zone_source가 MANUAL일 때)", example = "12")
    @JsonProperty("zone_id")
    private Long zoneId;
}
