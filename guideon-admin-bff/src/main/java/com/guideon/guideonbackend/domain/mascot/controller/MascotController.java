package com.guideon.guideonbackend.domain.mascot.controller;

import com.guideon.common.response.ApiResponse;
import com.guideon.guideonbackend.domain.mascot.dto.CreateMascotRequest;
import com.guideon.guideonbackend.domain.mascot.dto.MascotResponse;
import com.guideon.guideonbackend.domain.mascot.dto.UpdateMascotRequest;
import com.guideon.guideonbackend.domain.mascot.service.MascotService;
import com.guideon.guideonbackend.global.security.CustomAdminDetails;
import com.guideon.guideonbackend.global.trace.TraceIdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "마스코트 관리", description = "마스코트(Mascot) 설정 API - PLATFORM_ADMIN 전용")
@RestController
@RequestMapping("/api/v1/admin/sites/{siteId}/mascot")
@RequiredArgsConstructor
public class MascotController {

    private final MascotService mascotService;

    @Operation(summary = "마스코트 생성", description = "관광지에 마스코트를 등록합니다. PLATFORM_ADMIN 권한 필요")
    @PostMapping
    public ResponseEntity<ApiResponse<MascotResponse>> createMascot(
            @PathVariable Long siteId,
            @Valid @RequestBody CreateMascotRequest request,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        MascotResponse response = mascotService.createMascot(siteId, request, adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    @Operation(summary = "마스코트 조회", description = "관광지의 마스코트 설정을 조회합니다. PLATFORM_ADMIN 권한 필요")
    @GetMapping
    public ResponseEntity<ApiResponse<MascotResponse>> getMascot(
            @PathVariable Long siteId,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        MascotResponse response = mascotService.getMascot(siteId, adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    @Operation(summary = "마스코트 수정", description = "마스코트 설정을 수정합니다. 전송한 필드만 수정됩니다. PLATFORM_ADMIN 권한 필요")
    @PatchMapping
    public ResponseEntity<ApiResponse<MascotResponse>> updateMascot(
            @PathVariable Long siteId,
            @Valid @RequestBody UpdateMascotRequest request,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        MascotResponse response = mascotService.updateMascot(siteId, request, adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }
}
