package com.guideon.guideonbackend.domain.mascot.controller;

import com.guideon.common.response.ApiResponse;
import com.guideon.guideonbackend.domain.mascot.dto.*;
import com.guideon.guideonbackend.domain.mascot.service.MascotGenerationService;
import com.guideon.guideonbackend.domain.mascot.service.MascotService;
import com.guideon.guideonbackend.global.security.CustomAdminDetails;
import com.guideon.guideonbackend.global.trace.TraceIdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "마스코트 관리", description = "마스코트(Mascot) 설정 API - PLATFORM_ADMIN 전용")
@RestController
@RequestMapping("/api/v1/admin/sites/{siteId}/mascot")
@RequiredArgsConstructor
public class MascotController {

    private final MascotService mascotService;
    private final MascotGenerationService generationService;

    @Operation(summary = "마스코트 이미지 업로드", description = "마스코트 이미지를 업로드하고 URL을 반환합니다. PLATFORM_ADMIN 권한 필요")
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MascotImageUploadResponse>> uploadMascotImage(
            @PathVariable Long siteId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        MascotImageUploadResponse response = mascotService.uploadMascotImage(siteId, file, adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

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

    // ── 3D 모델 생성 (Tripo AI) ──

    @Operation(summary = "3D 모델 생성 시작",
            description = "선업로드된 이미지 URL(POST /mascot/image 응답)을 받아 Tripo AI 3D 모델 생성을 시작합니다. PLATFORM_ADMIN 권한 필요")
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<StartGenerationResponse>> startGeneration(
            @PathVariable Long siteId,
            @Valid @RequestBody StartGenerationRequest request,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        StartGenerationResponse response = generationService.startGeneration(siteId, request.getImageUrl(), adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    @Operation(summary = "3D 모델 생성 상태 폴링", description = "생성 진행 상태를 확인합니다. model 완료 시 자동으로 rigging이 시작됩니다.")
    @GetMapping("/generate/{generationId}/status")
    public ResponseEntity<ApiResponse<GenerationStatusResponse>> pollGenerationStatus(
            @PathVariable Long siteId,
            @PathVariable Long generationId,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        GenerationStatusResponse response = generationService.pollStatus(siteId, generationId, adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    @Operation(summary = "최근 3D 생성 이력 조회", description = "해당 사이트의 가장 최근 3D 모델 생성 상태를 조회합니다.")
    @GetMapping("/generate/latest")
    public ResponseEntity<ApiResponse<GenerationStatusResponse>> getLatestGeneration(
            @PathVariable Long siteId,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        GenerationStatusResponse response = generationService.getLatestGeneration(siteId, adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }
}
