package com.guideon.core.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class UpdateDeviceCommand {

    private String locationName;
    private Double latitude;
    private Double longitude;
    private Boolean isActive;
    private String zoneSource; // 변경 시 zone 재할당 트리거
    private Long zoneId;       // MANUAL일 때 지정
}
