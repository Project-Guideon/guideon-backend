package com.guideon.core.dto.device;

import com.guideon.core.domain.device.entity.Device;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceDto {

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

    public static DeviceDto from(Device device) {
        return DeviceDto.builder()
                .deviceId(device.getDeviceId())
                .siteId(device.getSite().getSiteId())
                .zoneId(device.getZone() != null ? device.getZone().getZoneId() : null)
                .zoneSource(device.getZoneSource().name())
                .locationName(device.getLocationName())
                .latitude(device.getLocation().getY())
                .longitude(device.getLocation().getX())
                .isActive(device.getIsActive())
                .lastPing(device.getLastPing())
                .lastAuthAt(device.getLastAuthAt())
                .createdAt(device.getCreatedAt())
                .updatedAt(device.getUpdatedAt())
                .build();
    }
}
