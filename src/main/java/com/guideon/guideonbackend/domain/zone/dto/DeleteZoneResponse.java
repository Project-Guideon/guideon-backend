package com.guideon.guideonbackend.domain.zone.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "Zone 삭제 응답")
public class DeleteZoneResponse {

    @Schema(description = "삭제 성공 여부", example = "true")
    private Boolean deleted;

    @JsonProperty("zone_id")
    @Schema(description = "삭제된 Zone ID", example = "12")
    private Long zoneId;

    @Schema(description = "경고 메시지 (INNER 삭제 시 자식 SUB도 함께 삭제됨)", example = "INNER 구역 삭제로 인해 3개의 하위 SUB 구역도 함께 삭제되었습니다.")
    private String warning;

    public static DeleteZoneResponse of(Long zoneId, String warning) {
        return DeleteZoneResponse.builder()
                .deleted(true)
                .zoneId(zoneId)
                .warning(warning)
                .build();
    }

    public static DeleteZoneResponse of(Long zoneId) {
        return DeleteZoneResponse.builder()
                .deleted(true)
                .zoneId(zoneId)
                .build();
    }
}
