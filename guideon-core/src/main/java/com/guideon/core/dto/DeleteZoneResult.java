package com.guideon.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Zone 삭제 결과
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteZoneResult {
    private Long deletedZoneId;

    public static DeleteZoneResult of(Long zoneId) {
        return DeleteZoneResult.builder()
                .deletedZoneId(zoneId)
                .build();
    }
}
