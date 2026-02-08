package com.guideon.guideonbackend.domain.place.dto;

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
@Schema(description = "장소 생성 요청")
public class CreatePlaceRequest {

    @Schema(description = "기본 장소명", example = "근정전 화장실")
    @NotBlank(message = "장소 이름은 필수입니다")
    @Size(max = 100, message = "장소 이름은 100자 이하여야 합니다")
    private String name;

    @Schema(description = "다국어 장소명", example = "{\"en\": \"Geunjeongjeon Restroom\", \"ja\": \"トイレ\"}")
    @JsonProperty("name_json")
    private Map<String, String> nameJson;

    @Schema(description = "카테고리", example = "TOILET")
    @NotBlank(message = "카테고리는 필수입니다")
    @Size(max = 50, message = "카테고리는 50자 이하여야 합니다")
    private String category;

    @Schema(description = "위도", example = "37.5796")
    @NotNull(message = "위도는 필수입니다")
    private Double latitude;

    @Schema(description = "경도", example = "126.9770")
    @NotNull(message = "경도는 필수입니다")
    private Double longitude;

    @Schema(description = "설명", example = "근정전 우측 50m")
    private String description;

    @Schema(description = "썸네일 URL", example = "https://.../toilet.png")
    @JsonProperty("image_url")
    @Size(max = 500, message = "이미지 URL은 500자 이하여야 합니다")
    private String imageUrl;

    @Schema(description = "활성화 여부", example = "true", defaultValue = "true")
    @JsonProperty("is_active")
    private Boolean isActive;

    @Schema(description = "구역 할당 방식 (AUTO/MANUAL)", example = "AUTO", defaultValue = "AUTO")
    @JsonProperty("zone_source")
    private String zoneSource;
}
