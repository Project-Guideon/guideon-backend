package com.guideon.core.dto.pairing;

import com.guideon.core.domain.pairing.entity.PairingRequest;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PairingStatusResponse {
    private String pairingCode;
    private String status;

    public static PairingStatusResponse waiting(PairingRequest request) {
        return PairingStatusResponse.builder()
                .pairingCode(request.getPairingCode())
                .status(request.getStatus().name())
                .build();
    }

    public static PairingStatusResponse from(PairingRequest request) {
        return PairingStatusResponse.builder()
                .pairingCode(request.getPairingCode())
                .status(request.getStatus().name())
                .build();
    }
}
