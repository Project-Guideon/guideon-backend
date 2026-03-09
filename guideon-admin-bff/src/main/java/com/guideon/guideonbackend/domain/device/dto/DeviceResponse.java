package com.guideon.guideonbackend.domain.device.dto;

import com.guideon.core.dto.DeviceDto;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DeviceResponse {

    private String deviceId;
    private Long siteId;
    private Long zoneId;
    private String zoneSource;
    private String locationName;
    private Double latitude;
    private Double longitude;
    private Boolean isActive;
    private LocalDateTime lastPing;
    private LocalDateTime lastAuthAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DeviceResponse from(DeviceDto dto) {
        return DeviceResponse.builder()
                .deviceId(dto.getDeviceId())
                .siteId(dto.getSiteId())
                .zoneId(dto.getZoneId())
                .zoneSource(dto.getZoneSource())
                .locationName(dto.getLocationName())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .isActive(dto.getIsActive())
                .lastPing(dto.getLastPing())
                .lastAuthAt(dto.getLastAuthAt())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }
}
