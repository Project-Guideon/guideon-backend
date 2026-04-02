package com.guideon.kiosk.client;

import com.guideon.core.dto.pairing.PairingClaimResponse;
import com.guideon.core.dto.pairing.PairingCodeResponse;
import com.guideon.core.dto.pairing.PairingStatusResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "core-pairing", url = "${core.service.url}")
public interface CorePairingClient {

    @PostMapping("/internal/v1/pairing/request")
    PairingCodeResponse requestPairingCode();

    @GetMapping("/internal/v1/pairing/{pairingCode}/status")
    PairingStatusResponse getPairingStatus(@PathVariable String pairingCode);

    @PostMapping("/internal/v1/pairing/{pairingCode}/claim")
    PairingClaimResponse claimPairingResult(@PathVariable("pairingCode") String pairingCode, @org.springframework.web.bind.annotation.RequestBody com.guideon.core.dto.pairing.PairingClaimCommand command);
}
