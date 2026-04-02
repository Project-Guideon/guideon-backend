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

    public static PairingCodeResponse of(PairingRequest request, String plainSecret) {
        return PairingCodeResponse.builder()
                .pairingCode(request.getPairingCode())
                .secret(plainSecret)
                .status(request.getStatus().name())
                .expiresAt(request.getExpiresAt())
                .build();
    }
}
