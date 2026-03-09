package com.guideon.guideonbackend.domain.device.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;

@Getter
public class UpdateDeviceRequest {

    @Size(max = 100, message = "locationName은 100자 이하여야 합니다")
    private String locationName;

    @DecimalMin(value = "-90.0", message = "latitude는 -90 이상이어야 합니다")
    @DecimalMax(value = "90.0", message = "latitude는 90 이하여야 합니다")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "longitude는 -180 이상이어야 합니다")
    @DecimalMax(value = "180.0", message = "longitude는 180 이하여야 합니다")
    private Double longitude;

    private Boolean isActive;

    /** AUTO 또는 MANUAL. MANUAL일 때는 zoneId 필수 */
    @Pattern(regexp = "^(AUTO|MANUAL)$", message = "zoneSource는 AUTO 또는 MANUAL이어야 합니다")
    private String zoneSource;

    /** zoneSource가 MANUAL일 때 지정 */
    private Long zoneId;
}
