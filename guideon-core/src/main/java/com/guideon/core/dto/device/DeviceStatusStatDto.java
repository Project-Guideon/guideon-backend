package com.guideon.core.dto.device;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeviceStatusStatDto {
    private final long total;
    private final long normal;
    private final long maintenance;
    private final long failure;
}
