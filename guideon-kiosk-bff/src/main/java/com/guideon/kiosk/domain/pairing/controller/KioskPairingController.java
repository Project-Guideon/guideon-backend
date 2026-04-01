package com.guideon.kiosk.domain.pairing.controller;

import com.guideon.common.response.ApiResponse;
import com.guideon.core.dto.pairing.PairingCodeResponse;
import com.guideon.core.dto.pairing.PairingStatusResponse;
import com.guideon.kiosk.client.CorePairingClient;
import com.guideon.kiosk.global.trace.TraceIdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "키오스크 페어링", description = "키오스크 첫 부팅 시 페어링 코드 기반 등록")
@RestController
@RequestMapping("/kiosk/pairing")
@RequiredArgsConstructor
public class KioskPairingController {

    private final CorePairingClient corePairingClient;

    /**
     * POST /kiosk/pairing/request
     * 키오스크 첫 부팅 → 6자리 페어링 코드 발급
     * 인증 불필요 (아직 토큰이 없는 상태)
     */
    @Operation(summary = "페어링 코드 요청", description = "키오스크 첫 부팅 시 6자리 페어링 코드를 발급받습니다. 인증 불필요.")
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<PairingCodeResponse>> requestPairingCode(
            HttpServletRequest httpRequest
    ) {
        PairingCodeResponse response = corePairingClient.requestPairingCode();
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    /**
     * GET /kiosk/pairing/{pairingCode}/status
     * 키오스크가 주기적으로 폴링하여 매칭 완료 여부 확인
     * 인증 불필요
     */
    @Operation(summary = "페어링 상태 확인", description = "키오스크가 폴링하여 관리자 매칭 완료 여부를 확인합니다.")
    @GetMapping("/{pairingCode}/status")
    public ResponseEntity<ApiResponse<PairingStatusResponse>> getPairingStatus(
            @PathVariable String pairingCode,
            HttpServletRequest httpRequest
    ) {
        PairingStatusResponse response = corePairingClient.getPairingStatus(pairingCode);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    /**
     * POST /kiosk/pairing/{pairingCode}/claim
     * PAIRED 확인 후 토큰 수령 → 키오스크가 로컬에 저장
     * 인증 불필요
     */
    @Operation(summary = "페어링 결과 수령", description = "매칭 완료 후 디바이스 토큰을 수령합니다. 이 토큰을 로컬에 저장하세요.")
    @PostMapping("/{pairingCode}/claim")
    public ResponseEntity<ApiResponse<PairingStatusResponse>> claimPairingResult(
            @PathVariable String pairingCode,
            HttpServletRequest httpRequest
    ) {
        PairingStatusResponse response = corePairingClient.claimPairingResult(pairingCode);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }
}
