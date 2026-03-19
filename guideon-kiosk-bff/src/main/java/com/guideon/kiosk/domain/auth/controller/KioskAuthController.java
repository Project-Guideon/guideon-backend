package com.guideon.kiosk.domain.auth.controller;

import com.guideon.common.response.ApiResponse;
import com.guideon.core.dto.kiosk.KioskBootstrapDto;
import com.guideon.core.dto.kiosk.KioskHeartbeatCommand;
import com.guideon.core.dto.kiosk.KioskMascotDto;
import com.guideon.kiosk.client.CoreKioskClient;
import com.guideon.kiosk.domain.auth.dto.HeartbeatRequest;
import com.guideon.kiosk.domain.auth.dto.VerifyResponse;
import com.guideon.kiosk.global.security.DeviceDetails;
import com.guideon.kiosk.global.trace.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/kiosk")
@RequiredArgsConstructor
public class KioskAuthController {

    private final CoreKioskClient coreKioskClient;

    /**
     * POST /kiosk/auth/verify
     * 디바이스 토큰 유효성 확인 + lastAuthAt 갱신
     * DeviceTokenAuthFilter가 인증 처리 → 여기 도달 == 유효한 토큰
     */
    @PostMapping("/auth/verify")
    public ResponseEntity<ApiResponse<VerifyResponse>> verify(
            @AuthenticationPrincipal DeviceDetails device,
            HttpServletRequest httpRequest
    ) {
        coreKioskClient.updateLastAuthAt(device.getDeviceId());
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(VerifyResponse.from(device), traceId));
    }

    /**
     * GET /kiosk/bootstrap
     * 디바이스 초기 설정 데이터 조회 (site/zone/mascot 컨텍스트)
     */
    @GetMapping("/bootstrap")
    public ResponseEntity<ApiResponse<KioskBootstrapDto>> bootstrap(
            @AuthenticationPrincipal DeviceDetails device,
            HttpServletRequest httpRequest
    ) {
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(
                coreKioskClient.getBootstrap(device.getDeviceId()), traceId
        ));
    }

    /**
     * GET /kiosk/mascot
     * 마스코트 상세 정보 조회 (2D 이미지 + 3D 모델 URL 포함)
     */
    @GetMapping("/mascot")
    public ResponseEntity<ApiResponse<KioskMascotDto>> getMascot(
            @AuthenticationPrincipal DeviceDetails device,
            HttpServletRequest httpRequest
    ) {
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(
                coreKioskClient.getMascot(device.getDeviceId()), traceId
        ));
    }

    /**
     * POST /kiosk/heartbeat
     * 하트비트 전송 (lastPing 갱신)
     */
    @PostMapping("/heartbeat")
    public ResponseEntity<ApiResponse<Void>> heartbeat(
            @AuthenticationPrincipal DeviceDetails device,
            @RequestBody(required = false) HeartbeatRequest request,
            HttpServletRequest httpRequest
    ) {
        KioskHeartbeatCommand command = request != null
                ? KioskHeartbeatCommand.builder()
                        .version(request.getVersion())
                        .errorCode(request.getErrorCode())
                        .build()
                : KioskHeartbeatCommand.builder().build();

        coreKioskClient.heartbeat(device.getDeviceId(), command);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(null, traceId));
    }
}
