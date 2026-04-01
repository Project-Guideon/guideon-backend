package com.guideon.guideonbackend.domain.device.controller;

import com.guideon.common.response.ApiResponse;
import com.guideon.common.response.PageResponse;
import com.guideon.guideonbackend.domain.device.dto.*;
import com.guideon.guideonbackend.domain.device.service.DeviceService;
import com.guideon.guideonbackend.global.security.CustomAdminDetails;
import com.guideon.guideonbackend.global.trace.TraceIdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "디바이스 관리", description = "키오스크 디바이스 CRUD + 토큰 재발급 API")
@RestController
@RequestMapping("/api/v1/admin/sites/{siteId}/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @Operation(summary = "디바이스 목록 조회", description = "관광지 내 디바이스 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DeviceResponse>>> getDevices(
            @PathVariable Long siteId,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        PageResponse<DeviceResponse> response = deviceService.getDevices(siteId, pageable, adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    @Operation(summary = "디바이스 상세 조회", description = "특정 디바이스의 상세 정보를 조회합니다.")
    @GetMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<DeviceResponse>> getDevice(
            @PathVariable Long siteId,
            @PathVariable String deviceId,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        DeviceResponse response = deviceService.getDevice(siteId, deviceId, adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    @Operation(summary = "디바이스 수정", description = "디바이스 정보를 수정합니다. 전송한 필드만 수정됩니다.")
    @PatchMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<DeviceResponse>> updateDevice(
            @PathVariable Long siteId,
            @PathVariable String deviceId,
            @Valid @RequestBody UpdateDeviceRequest request,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        DeviceResponse response = deviceService.updateDevice(siteId, deviceId, request, adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    @Operation(summary = "디바이스 삭제", description = "디바이스를 비활성화합니다 (soft delete).")
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<Void>> deleteDevice(
            @PathVariable Long siteId,
            @PathVariable String deviceId,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        deviceService.deleteDevice(siteId, deviceId, adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(null, traceId));
    }

    @Operation(summary = "페어링 코드로 디바이스 등록", description = "키오스크 화면에 표시된 6자리 페어링 코드로 디바이스를 등록합니다.")
    @PostMapping("/pair")
    public ResponseEntity<ApiResponse<DeviceResponse>> pairDevice(
            @PathVariable Long siteId,
            @Valid @RequestBody PairDeviceRequest request,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        DeviceResponse response = deviceService.pairDevice(siteId, request, adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    @Operation(summary = "디바이스 토큰 재발급", description = "디바이스 인증 토큰을 재발급합니다. 응답의 plainToken은 일회성이므로 즉시 저장하세요.")
    @PostMapping("/{deviceId}/rotate-token")
    public ResponseEntity<ApiResponse<RotateTokenResponse>> rotateToken(
            @PathVariable Long siteId,
            @PathVariable String deviceId,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        RotateTokenResponse response = deviceService.rotateToken(siteId, deviceId, adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }
}
