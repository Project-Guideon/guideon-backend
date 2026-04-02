package com.guideon.core.dto.pairing;

import com.guideon.core.domain.pairing.entity.PairingRequest;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PairingCodeResponse {
    private String pairingCode;
    private String secret;
    private String status;
    private LocalDateTime expiresAt;

    public static PairingCodeResponse from(PairingRequest request) {
        return PairingCodeResponse.builder()
                .pairingCode(request.getPairingCode())
                .secret(request.getSecret())
                .status(request.getStatus().name())
                .expiresAt(request.getExpiresAt())
                .build();
    }
}
