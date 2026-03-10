package com.guideon.kiosk.domain.auth.dto;

import com.guideon.kiosk.global.security.DeviceDetails;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VerifyResponse {
    private String deviceId;
    private Long siteId;
    private Long zoneId;

    public static VerifyResponse from(DeviceDetails details) {
        return VerifyResponse.builder()
                .deviceId(details.getDeviceId())
                .siteId(details.getSiteId())
                .zoneId(details.getZoneId())
                .build();
    }
}