package com.guideon.core.dto.zone;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecalcResultDto {

    private Long siteId;
    private int totalPlaces;
    private int updatedPlaces;
    private int totalDevices;
    private int updatedDevices;

    public static RecalcResultDto of(Long siteId, int totalPlaces, int updatedPlaces,
                                      int totalDevices, int updatedDevices) {
        return RecalcResultDto.builder()
                .siteId(siteId)
                .totalPlaces(totalPlaces)
                .updatedPlaces(updatedPlaces)
                .totalDevices(totalDevices)
                .updatedDevices(updatedDevices)
                .build();
    }
}
