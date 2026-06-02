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
 * 파이프라인: image_to_model → animate_rig → animate_retarget(5클립 배열) → GLB 저장
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
     * Phase 1 (model)  → success: animate_rig 시작
     * Phase 2 (rig)    → success: base GLB 저장 + animate_retarget(5클립 배열) 시작
     * Phase 3 (retarget) → success: anim GLB 저장 (5클립 내장)
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
                // base GLB 다운로드 → 저장
                byte[] glbBytes = tripoApiService.downloadModel(status.modelUrl());
                String glbHash = FileValidator.computeFileHash(glbBytes);
                String modelUrl = fileStorageService.store(siteId, glbHash, glbBytes, "mascot.glb");
                gen = persistService.applyRigComplete(siteId, generationId, modelUrl);
                log.info("base GLB 저장 완료, retarget 시작: generationId={}", generationId);

                // animate_retarget: 5클립 배열 1개 task
                String retargetTaskId = tripoApiService.createAnimateRetargetTask(
                        gen.getRigTaskId(), MascotMotion.presetList());
                gen = persistService.applyRetargetStarted(generationId, retargetTaskId);
            }

            return GenerationStatusResponse.from(gen);
        }

        // Phase 3: retarget 중 (단일 task 폴링)
        if (gen.getRetargetStatus() == GenerationStatus.PROCESSING) {
            TripoApiService.TripoTaskStatus status = tripoApiService.getTaskStatus(gen.getRetargetTaskId());

            if (status.isFailed()) {
                gen = persistService.applyRetargetFailed(generationId, "Tripo retarget 실패");
                return GenerationStatusResponse.from(gen);
            }

            if (status.isSuccess() && status.modelUrl() != null) {
                // anim GLB(5클립 내장) 다운로드 → 저장
                byte[] animBytes = tripoApiService.downloadModel(status.modelUrl());
                String animHash = FileValidator.computeFileHash(animBytes);
                String animModelUrl = fileStorageService.store(siteId, animHash, animBytes, "mascot_anim.glb");
                gen = persistService.applyRetargetComplete(
                        siteId, generationId, animModelUrl, MascotMotion.clipMap());
                log.info("마스코트 생성 완전 완료: generationId={}, animModelUrl={}", generationId, animModelUrl);
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
