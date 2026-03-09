package com.guideon.guideonbackend.domain.device.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;

@Getter
public class CreateDeviceRequest {

    @NotBlank(message = "deviceId는 필수입니다")
    @Size(max = 50, message = "deviceId는 50자 이하여야 합니다")
    private String deviceId;

    @NotBlank(message = "locationName은 필수입니다")
    @Size(max = 100, message = "locationName은 100자 이하여야 합니다")
    private String locationName;

    @NotNull(message = "latitude는 필수입니다")
    @DecimalMin(value = "-90.0", message = "latitude는 -90 이상이어야 합니다")
    @DecimalMax(value = "90.0", message = "latitude는 90 이하여야 합니다")
    private Double latitude;

    @NotNull(message = "longitude는 필수입니다")
    @DecimalMin(value = "-180.0", message = "longitude는 -180 이상이어야 합니다")
    @DecimalMax(value = "180.0", message = "longitude는 180 이하여야 합니다")
    private Double longitude;

    /** AUTO(기본) 또는 MANUAL. MANUAL일 때는 zoneId 필수 */
    @Pattern(regexp = "^(AUTO|MANUAL)$", message = "zoneSource는 AUTO 또는 MANUAL이어야 합니다")
    private String zoneSource;

    /** zoneSource가 MANUAL일 때 지정 */
    private Long zoneId;

    private Boolean isActive;
}
