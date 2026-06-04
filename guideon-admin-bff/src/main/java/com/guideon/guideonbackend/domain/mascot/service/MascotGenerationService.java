package com.guideon.guideonbackend.domain.mascot.service;

import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import com.guideon.core.domain.admin.entity.AdminRole;
import com.guideon.core.domain.mascot.entity.GenerationStatus;
import com.guideon.core.domain.mascot.entity.MascotGeneration;
import com.guideon.core.domain.mascot.repository.MascotGenerationRepository;
import com.guideon.core.domain.site.entity.Site;
import com.guideon.core.domain.site.repository.SiteRepository;
import com.guideon.guideonbackend.domain.mascot.dto.GenerationStatusResponse;
import com.guideon.guideonbackend.domain.mascot.dto.StartGenerationResponse;
import com.guideon.guideonbackend.global.security.CustomAdminDetails;
import com.guideon.guideonbackend.global.storage.FileStorageService;
import com.guideon.guideonbackend.global.storage.FileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * 마스코트 3D 모델 생성 파이프라인 오케스트레이션.
 *
 * 파이프라인: image_to_model → animate_rig(spec:mixamo) → 리깅 GLB 저장
 *            → (anim_config 설정 시) mesh-processor 자동 병합 → animModelUrl 저장
 *
 * 외부 API 호출은 트랜잭션 밖에서 수행하고,
 * DB 저장/업데이트는 MascotGenerationPersistService를 통해 별도 트랜잭션으로 처리.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MascotGenerationService {

    private final TripoApiService tripoApiService;
    private final MascotGenerationPersistService persistService;
    private final MascotGenerationRepository generationRepository;
    private final SiteRepository siteRepository;
    private final FileStorageService fileStorageService;
    private final MascotAnimationMergeService animationMergeService;
    private final MeshProcessorClient meshProcessorClient;

    /**
     * Step 1: 선업로드된 이미지 URL을 받아 Tripo 3D 생성 task 시작
     */
    public StartGenerationResponse startGeneration(Long siteId, String imageUrl,
                                                    CustomAdminDetails adminDetails) {
        validatePlatformAdmin(adminDetails);

        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 관광지: " + siteId));

        // 진행 중인 생성 작업이 있는지 확인
        generationRepository.findTopBySite_SiteIdOrderByCreatedAtDesc(siteId)
                .filter(gen -> !gen.isFullyCompleted() && !gen.isFailed())
                .ifPresent(gen -> {
                    throw new CustomException(ErrorCode.CONFLICT,
                            "이미 진행 중인 3D 생성 작업이 있습니다: " + gen.getGenerationId());
                });

        byte[] imageBytes = fileStorageService.loadBytes(siteId, imageUrl);
        String filename = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);

        String imageToken = tripoApiService.uploadImage(imageBytes, filename);
        String modelTaskId = tripoApiService.createImageToModelTask(imageToken);

        MascotGeneration generation = persistService.saveNewGeneration(site, imageUrl, modelTaskId);

        log.info("마스코트 3D 생성 시작: generationId={}, siteId={}, modelTaskId={}",
                generation.getGenerationId(), siteId, modelTaskId);

        return StartGenerationResponse.builder()
                .generationId(generation.getGenerationId())
                .modelTaskId(modelTaskId)
                .status("PROCESSING")
                .build();
    }

    /**
     * Step 2: 생성 상태 폴링
     *
     * Phase 1 (model) → success: animate_rig(spec:mixamo) 시작
     * Phase 2 (rig)   → success: 리깅 GLB 저장 → 파이프라인 완료
     */
    public GenerationStatusResponse pollStatus(Long siteId, Long generationId,
                                                CustomAdminDetails adminDetails) {
        validatePlatformAdmin(adminDetails);

        MascotGeneration gen = persistService.loadAndValidate(siteId, generationId);

        if (gen.isFullyCompleted() || gen.isFailed()) {
            return GenerationStatusResponse.from(gen);
        }

        // Phase 1: model 생성 중
        if (gen.getModelStatus() == GenerationStatus.PROCESSING) {
            TripoApiService.TripoTaskStatus status = tripoApiService.getTaskStatus(gen.getModelTaskId());

            if (status.isFailed()) {
                gen = persistService.applyModelFailed(generationId, "Tripo model 생성 실패");
                log.warn("마스코트 model 생성 실패: generationId={}", generationId);
                return GenerationStatusResponse.from(gen);
            }

            if (status.isSuccess()) {
                log.info("마스코트 model 완료, rigging 시작: generationId={}", generationId);
                String rigTaskId = tripoApiService.createAnimateRigTask(gen.getModelTaskId());
                gen = persistService.applyModelComplete(generationId, rigTaskId);

                // pre-rig 모델 = 뼈대 없는 clean mesh → Mixamo 업로드용 FBX 확보
                // Tripo 반환 포맷(GLB/FBX) 무관하게 항상 clean_mesh_{id}.fbx가 생성되도록 처리.
                if (status.modelUrl() != null) {
                    try {
                        byte[] preRigBytes = tripoApiService.downloadModel(status.modelUrl());
                        if (FileValidator.isGlb(preRigBytes)) {
                            // GLB 반환: 저장 후 assimp로 FBX 변환 (stripRigAsync)
                            String preRigHash = FileValidator.computeFileHash(preRigBytes);
                            String preRigUrl  = fileStorageService.store(siteId, preRigHash, preRigBytes, "pre_rig_mascot.glb");
                            animationMergeService.stripRigAsync(siteId, generationId, preRigUrl);
                        } else {
                            // FBX 직접 반환: 변환 없이 clean_mesh FBX로 저장
                            log.info("pre-rig 결과 FBX 반환 — clean_mesh FBX 직접 저장: generationId={}", generationId);
                            String fbxFilename = "clean_mesh_" + generationId + ".fbx";
                            String fbxHash     = FileValidator.computeFileHash(preRigBytes);
                            String fbxUrl      = fileStorageService.store(siteId, fbxHash, preRigBytes, fbxFilename);
                            persistService.saveCleanMeshUrl(siteId, fbxUrl);
                        }
                    } catch (Exception e) {
                        log.warn("pre-rig 모델 처리 실패 (무시): generationId={}, err={}", generationId, e.getMessage());
                    }
                }
            }

            return GenerationStatusResponse.from(gen);
        }

        // Phase 2: rigging 중
        if (gen.getRigStatus() == GenerationStatus.PROCESSING) {
            TripoApiService.TripoTaskStatus status = tripoApiService.getTaskStatus(gen.getRigTaskId());

            if (status.isFailed()) {
                gen = persistService.applyRigFailed(generationId, "Tripo rigging 실패");
                log.warn("마스코트 rigging 실패: generationId={}", generationId);
                return GenerationStatusResponse.from(gen);
            }

            if (status.isSuccess() && status.modelUrl() != null) {
                // 리깅 결과 다운로드 → GLB/FBX 포맷 판별
                byte[] rigBytes = tripoApiService.downloadModel(status.modelUrl());
                String modelUrl;

                if (FileValidator.isGlb(rigBytes)) {
                    // 정상 케이스: GLB 그대로 저장
                    String glbHash = FileValidator.computeFileHash(rigBytes);
                    modelUrl = fileStorageService.store(siteId, glbHash, rigBytes, "mascot.glb");
                    log.info("리깅 GLB 저장 완료: generationId={}", generationId);
                } else {
                    // Tripo FBX 반환 케이스: mesh-processor /convert 로 GLB 변환
                    log.warn("Tripo rig 결과 FBX 반환 — /convert로 GLB 변환 시도: generationId={}", generationId);
                    try {
                        String fbxHash     = FileValidator.computeFileHash(rigBytes);
                        String fbxUrl      = fileStorageService.store(siteId, fbxHash, rigBytes,
                                                "rig_raw_" + generationId + ".fbx");
                        String fbxLocalPath = fileStorageService.toLocalPath(fbxUrl).toString();
                        String glbFilename  = "mascot.glb";
                        String glbUrl       = fileStorageService.resolveUrl(siteId, glbFilename);
                        String glbLocalPath = fileStorageService.toLocalPath(glbUrl).toString();
                        meshProcessorClient.convert(fbxLocalPath, glbLocalPath);
                        modelUrl = glbUrl;
                        log.info("FBX→GLB 변환 완료: generationId={}", generationId);
                    } catch (Exception e) {
                        log.error("FBX→GLB 변환 실패 — rig 실패 처리: generationId={}, err={}", generationId, e.getMessage());
                        gen = persistService.applyRigFailed(generationId,
                                "FBX→GLB 변환 실패: " + e.getMessage());
                        return GenerationStatusResponse.from(gen);
                    }
                }

                gen = persistService.applyRigComplete(siteId, generationId, modelUrl);

                // anim_config가 설정되어 있으면 mesh-processor로 자동 병합
                animationMergeService.mergeIfRiggedMascotExists(siteId, generationId, modelUrl);
            }

            return GenerationStatusResponse.from(gen);
        }

        return GenerationStatusResponse.from(gen);
    }

    /**
     * 생성 이력 조회 (최신)
     */
    @Transactional(readOnly = true)
    public GenerationStatusResponse getLatestGeneration(Long siteId, CustomAdminDetails adminDetails) {
        validatePlatformAdmin(adminDetails);

        MascotGeneration gen = generationRepository.findTopBySite_SiteIdOrderByCreatedAtDesc(siteId)
                .orElseThrow(() -> new CustomException(ErrorCode.MASCOT_GENERATION_NOT_FOUND));

        return GenerationStatusResponse.from(gen);
    }

    private void validatePlatformAdmin(CustomAdminDetails adminDetails) {
        if (!AdminRole.PLATFORM_ADMIN.name().equals(adminDetails.getRole())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
    }
}
