package com.guideon.guideonbackend.domain.zone.dto;

import com.guideon.core.dto.zone.RecalcResultDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecalcZoneResponse {

    private Long siteId;
    private int totalPlaces;
    private int updatedPlaces;
    private int totalDevices;
    private int updatedDevices;

    public static RecalcZoneResponse from(RecalcResultDto dto) {
        return RecalcZoneResponse.builder()
                .siteId(dto.getSiteId())
                .totalPlaces(dto.getTotalPlaces())
                .updatedPlaces(dto.getUpdatedPlaces())
                .totalDevices(dto.getTotalDevices())
                .updatedDevices(dto.getUpdatedDevices())
                .build();
    }
}
