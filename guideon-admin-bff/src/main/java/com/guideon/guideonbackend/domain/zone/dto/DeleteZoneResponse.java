package com.guideon.guideonbackend.domain.zone.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.guideon.core.dto.DeleteZoneResult;
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

    public static DeleteZoneResponse from(DeleteZoneResult result) {
        return DeleteZoneResponse.builder()
                .deleted(true)
                .zoneId(result.getDeletedZoneId())
                .build();
    }
}
