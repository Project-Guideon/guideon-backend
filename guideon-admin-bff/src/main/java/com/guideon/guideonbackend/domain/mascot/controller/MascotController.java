package com.guideon.guideonbackend.domain.mascot.controller;

import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import com.guideon.common.response.ApiResponse;
import com.guideon.guideonbackend.domain.mascot.dto.CleanMeshResponse;
import com.guideon.guideonbackend.domain.mascot.dto.ModelUploadResponse;
import com.guideon.guideonbackend.domain.mascot.dto.*;
import org.springframework.lang.Nullable;
import com.guideon.guideonbackend.domain.mascot.service.MascotAnimConfigService;
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
    private final MascotAnimConfigService animConfigService;

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

    @Operation(summary = "Mixamo 업로드용 Clean Mesh 조회",
            description = "스켈레톤 제거된 T-포즈 FBX URL을 반환합니다. " +
                    "이 파일을 Mixamo에 업로드하면 자동 리깅 후 애니메이션을 다운로드할 수 있습니다. " +
                    "rig 완료 후 자동 생성됩니다. PLATFORM_ADMIN 권한 필요")
    @GetMapping("/clean-mesh")
    public ResponseEntity<ApiResponse<CleanMeshResponse>> getCleanMesh(
            @PathVariable Long siteId,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        CleanMeshResponse response = mascotService.getCleanMesh(siteId, adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    // ── 애니메이션 GLB 업로드 ──

    @Operation(
            summary = "Mixamo 애니메이션 GLB 업로드",
            description = "Mixamo + Blender로 제작한 통합 anim GLB를 업로드하고 마스코트에 연결합니다. " +
                    "animClips를 함께 전달하지 않으면 기본 매핑(Idle/Talking/Waving)이 사용됩니다. " +
                    "PLATFORM_ADMIN 권한 필요")
    @PostMapping(value = "/animation", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AnimationUploadResponse>> uploadAnimation(
            @PathVariable Long siteId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "animClips", required = false) @Nullable String animClipsJson,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        AnimationUploadResponse response = mascotService.uploadAnimation(siteId, file, animClipsJson, adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    // ── 음성 클로닝 (Cartesia) ──

    @Operation(
            summary = "마스코트 음성 클로닝",
            description = "음성 샘플 파일을 업로드하여 Cartesia 커스텀 보이스를 생성합니다. " +
                    "생성된 voice_id는 마스코트 ttsVoiceId에 자동 저장됩니다. PLATFORM_ADMIN 권한 필요")
    @PostMapping(value = "/voice/clone", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VoiceCloneResponse>> cloneVoice(
            @PathVariable Long siteId,
            @RequestPart("file") MultipartFile audioFile,
            @RequestParam("name") String name,
            @RequestParam(value = "language", defaultValue = "ko") String language,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        if (name == null || name.isBlank()) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "보이스 이름은 필수입니다.");
        }
        if (language == null || language.isBlank()) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "언어 코드는 필수입니다.");
        }
        VoiceCloneResponse response = mascotService.cloneVoice(siteId, audioFile, name, language, adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    // ── 애니메이션 설정 (Mixamo GLB 업로드 / 매핑 관리) ──

    @Operation(
            summary = "상태별 애니메이션 GLB 업로드",
            description = "Mixamo에서 준비한 state별 GLB 파일을 업로드합니다. " +
                    "업로드 후 마스코트 생성(rig 완료) 시 자동으로 anim GLB가 병합됩니다. " +
                    "각 state 파트명: idle / speaking / listening / thinking / greeting. " +
                    "PLATFORM_ADMIN 권한 필요")
    @PostMapping(value = "/animations", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AnimationGlbsUploadResponse>> uploadAnimationGlbs(
            @PathVariable Long siteId,
            @RequestPart(value = "idle",      required = false) MultipartFile idle,
            @RequestPart(value = "speaking",  required = false) MultipartFile speaking,
            @RequestPart(value = "listening", required = false) MultipartFile listening,
            @RequestPart(value = "thinking",  required = false) MultipartFile thinking,
            @RequestPart(value = "greeting",  required = false) MultipartFile greeting,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        java.util.Map<String, MultipartFile> files = new java.util.LinkedHashMap<>();
        if (idle      != null) files.put("idle",      idle);
        if (speaking  != null) files.put("speaking",  speaking);
        if (listening != null) files.put("listening", listening);
        if (thinking  != null) files.put("thinking",  thinking);
        if (greeting  != null) files.put("greeting",  greeting);

        AnimationGlbsUploadResponse response = animConfigService.uploadAnimationGlbs(siteId, files, adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    @Operation(summary = "애니메이션 설정 조회", description = "현재 상태→클립명 매핑과 GLB URL을 조회합니다.")
    @GetMapping("/anim-config")
    public ResponseEntity<ApiResponse<AnimConfigResponse>> getAnimConfig(
            @PathVariable Long siteId,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        AnimConfigResponse response = animConfigService.getAnimConfig(siteId, adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    @Operation(summary = "애니메이션 클립명 수정",
            description = "GLB 파일 변경 없이 상태→클립명 매핑만 수정합니다.")
    @PutMapping("/anim-config")
    public ResponseEntity<ApiResponse<AnimConfigResponse>> updateAnimConfig(
            @PathVariable Long siteId,
            @RequestBody AnimConfigRequest request,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        AnimConfigResponse response = animConfigService.updateAnimConfig(siteId, request, adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    // ── 수동 GLB 업로드 ──

    @Operation(
            summary = "마스코트 GLB 수동 업로드 (모델 교체)",
            description = "외부에서 준비한 GLB를 업로드해 마스코트 model_url을 교체합니다. " +
                    "anim_config가 설정되어 있으면 anim 병합도 자동 실행됩니다. " +
                    "Tripo 생성 파이프라인 없이 직접 모델을 교체할 때 사용. PLATFORM_ADMIN 권한 필요")
    @PostMapping(value = "/model", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ModelUploadResponse>> uploadMascotModel(
            @PathVariable Long siteId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        ModelUploadResponse response = generationService.uploadMascotModel(siteId, file, adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    // ── 3D 모델 생성 (Tripo AI) ──

    @Operation(summary = "3D 모델 생성 시작",
            description = "선업로드된 이미지 URL(POST /mascot/image 응답)을 받아 Tripo AI 3D 모델 생성 및 리깅을 시작합니다. " +
                    "완료 후 resultModelUrl로 리깅 GLB를 다운로드하여 Mixamo 애니메이션 작업에 사용합니다. PLATFORM_ADMIN 권한 필요")
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

    @Operation(summary = "3D 모델 생성 상태 폴링",
            description = "생성 진행 상태를 확인합니다. model 완료 시 자동으로 rigging(spec:mixamo)이 시작됩니다. " +
                    "completed=true가 되면 resultModelUrl에서 리깅 GLB를 다운로드할 수 있습니다.")
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
