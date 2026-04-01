package com.guideon.core.api;

import com.guideon.core.dto.device.DeviceDto;
import com.guideon.core.dto.pairing.PairDeviceCommand;
import com.guideon.core.dto.pairing.PairingClaimResponse;
import com.guideon.core.dto.pairing.PairingCodeResponse;
import com.guideon.core.dto.pairing.PairingStatusResponse;
import com.guideon.core.service.PairingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/v1/pairing")
@RequiredArgsConstructor
public class PairingInternalController {

    private final PairingService pairingService;

    /** 키오스크 첫 부팅 → 페어링 코드 발급 */
    @PostMapping("/request")
    public ResponseEntity<PairingCodeResponse> requestPairingCode() {
        return ResponseEntity.ok(pairingService.requestPairingCode());
    }

    /** 키오스크 폴링 → 페어링 상태 확인 */
    @GetMapping("/{pairingCode}/status")
    public ResponseEntity<PairingStatusResponse> getPairingStatus(
            @PathVariable String pairingCode) {
        return ResponseEntity.ok(pairingService.getPairingStatus(pairingCode));
    }

    /** 키오스크 → PAIRED 확인 후 토큰 수령 (1회만 가능) */
    @PostMapping("/{pairingCode}/claim")
    public ResponseEntity<PairingClaimResponse> claimPairingResult(
            @PathVariable String pairingCode) {
        return ResponseEntity.ok(pairingService.claimPairingResult(pairingCode));
    }

    /** 관리자 → 페어링 코드로 디바이스 매칭 */
    @PostMapping("/sites/{siteId}/pair")
    public ResponseEntity<DeviceDto> pairDevice(
            @PathVariable Long siteId,
            @RequestBody PairDeviceCommand command) {
        return ResponseEntity.ok(pairingService.pairDevice(siteId, command));
    }
}
