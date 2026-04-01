package com.guideon.core.dto.pairing;

import com.guideon.core.domain.pairing.entity.PairingRequest;
import com.guideon.core.dto.device.DeviceDto;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PairingStatusResponse {
    private String pairingCode;
    private String status;

    /** PAIRED 상태일 때만 포함 (키오스크가 이 토큰을 저장) */
    private String plainToken;
    private DeviceDto device;

    public static PairingStatusResponse waiting(PairingRequest request) {
        return PairingStatusResponse.builder()
                .pairingCode(request.getPairingCode())
                .status(request.getStatus().name())
                .build();
    }

    public static PairingStatusResponse paired(PairingRequest request, String plainToken, DeviceDto device) {
        return PairingStatusResponse.builder()
                .pairingCode(request.getPairingCode())
                .status(request.getStatus().name())
                .plainToken(plainToken)
                .device(device)
                .build();
    }
}
