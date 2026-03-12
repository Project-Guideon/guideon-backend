package com.guideon.core.dto.device;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class CreateDeviceCommand {

    private String deviceId;
    private String locationName;
    private Double latitude;
    private Double longitude;
    private String zoneSource; // "AUTO" or "MANUAL"
    private Long zoneId;       // MANUAL일 때 지정
    private Boolean isActive;
}
