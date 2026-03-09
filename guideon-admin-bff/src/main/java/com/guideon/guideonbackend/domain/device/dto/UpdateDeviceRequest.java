package com.guideon.guideonbackend.domain.device.dto;

import lombok.Getter;

@Getter
public class UpdateDeviceRequest {

    private String locationName;
    private Double latitude;
    private Double longitude;
    private Boolean isActive;
    private String zoneSource;
    private Long zoneId;
}
