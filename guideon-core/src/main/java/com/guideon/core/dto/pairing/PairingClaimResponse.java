package com.guideon.core.dto.pairing;

import com.guideon.core.dto.device.DeviceDto;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PairingClaimResponse {
    private String plainToken;
    private DeviceDto device;
}
