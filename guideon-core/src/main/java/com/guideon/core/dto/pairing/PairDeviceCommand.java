package com.guideon.core.dto.pairing;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class PairDeviceCommand {
    private String pairingCode;
    private String deviceId;
    private String locationName;
    private Double latitude;
    private Double longitude;
    private String zoneSource;
    private Long zoneId;
}
