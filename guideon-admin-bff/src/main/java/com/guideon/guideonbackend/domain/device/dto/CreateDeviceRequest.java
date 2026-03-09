package com.guideon.guideonbackend.domain.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class CreateDeviceRequest {

    @NotBlank(message = "deviceId는 필수입니다")
    private String deviceId;

    @NotBlank(message = "locationName은 필수입니다")
    private String locationName;

    @NotNull(message = "latitude는 필수입니다")
    private Double latitude;

    @NotNull(message = "longitude는 필수입니다")
    private Double longitude;

    /** AUTO(기본) 또는 MANUAL */
    private String zoneSource;

    /** zoneSource가 MANUAL일 때 지정 */
    private Long zoneId;

    private Boolean isActive;
}
