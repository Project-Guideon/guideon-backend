package com.guideon.guideonbackend.domain.site.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateSiteRequest {

    @NotBlank(message = "관광지 이름은 필수입니다")
    @Size(max = 100, message = "관광지 이름은 100자 이하여야 합니다")
    private String name;

    // 지도 중심 좌표 — 선택값 (null 허용, 입력 시 범위 검증)
    // 좌표를 모두 미전송(null)하면 기존 좌표 유지, 전송 시 갱신 (Core SiteService에서 처리)
    @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다")
    @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다")
    @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다")
    private Double longitude;

    @Min(value = 1, message = "지도 줌 레벨은 1 이상이어야 합니다")
    @Max(value = 14, message = "지도 줌 레벨은 14 이하여야 합니다")
    private Integer mapLevel;

    /** 위도·경도·줌 레벨은 반드시 함께 입력 (반쪽 좌표 방지) */
    @JsonIgnore
    @AssertTrue(message = "위도, 경도, 줌 레벨은 함께 입력해야 합니다")
    public boolean isCoordinatesPaired() {
        boolean latPresent = latitude != null;
        boolean lonPresent = longitude != null;
        boolean lvlPresent = mapLevel != null;
        // 셋 다 null 이거나, 셋 다 non-null 이어야 함
        return (latPresent == lonPresent) && (lonPresent == lvlPresent);
    }
}
