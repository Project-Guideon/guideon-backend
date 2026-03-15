package com.guideon.guideonbackend.domain.mascot.service;

import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import com.guideon.core.domain.admin.entity.AdminRole;
import com.guideon.core.domain.mascot.entity.Mascot;
import com.guideon.core.domain.mascot.entity.MascotGeneration;
import com.guideon.core.domain.mascot.repository.MascotGenerationRepository;
import com.guideon.core.domain.mascot.repository.MascotRepository;
import com.guideon.core.domain.site.entity.Site;
import com.guideon.core.domain.site.repository.SiteRepository;
import com.guideon.core.dto.mascot.MascotGenerationDto;
import com.guideon.guideonbackend.domain.mascot.dto.GenerationStatusResponse;
import com.guideon.guideonbackend.domain.mascot.dto.StartGenerationRequest;
import com.guideon.guideonbackend.domain.mascot.dto.StartGenerationResponse;
import com.guideon.guideonbackend.global.security.CustomAdminDetails;
import com.guideon.guideonbackend.global.storage.FileStorageService;
import com.guideon.guideonbackend.global.storage.FileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 마스코트 3D 모델 생성 파이프라인 오케스트레이션.
 *
 * 흐름:
 * 1) startGeneration: 이미지 업로드 → Tripo image_to_model task 생성 → DB 저장
 * 2) pollStatus: task 상태 폴링 → model 완료 시 rig task 자동 생성 → rig 완료 시 GLB 다운로드+저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MascotGenerationService {

    private final TripoApiService tripoApiService;
    private final MascotGenerationRepository generationRepository;
    private final MascotRepository mascotRepository;
    private final SiteRepository siteRepository;
    private final FileStorageService fileStorageService;

    /**
     * Step 1: 이미지 업로드 + Tripo 3D 생성 task 시작
     */
    @Transactional
    public StartGenerationResponse startGeneration(Long siteId, MultipartFile imageFile,
                                                    CustomAdminDetails adminDetails) {
        validatePlatformAdmin(adminDetails);
        FileValidator.validateImage(imageFile);

        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 관광지: " + siteId));

        // 1. 이미지 로컬 저장 (원본 보관)
        String fileHash = FileValidator.computeFileHash(imageFile);
        String sourceImageUrl = fileStorageService.store(siteId, fileHash, imageFile);

        // 2. Tripo에 이미지 업로드 → file token
        String imageToken = tripoApiService.uploadImage(imageFile);

        // 3. image_to_model task 생성
        String modelTaskId = tripoApiService.createImageToModelTask(imageToken);

        // 4. DB 저장
        MascotGeneration generation = MascotGeneration.builder()
                .site(site)
                .sourceImageUrl(sourceImageUrl)
                .build();
        generation.startModelGeneration(modelTaskId);
        generationRepository.save(generation);

        log.info("마스코트 3D 생성 시작: generationId={}, siteId={}, modelTaskId={}",
                generation.getGenerationId(), siteId, modelTaskId);

        return StartGenerationResponse.builder()
                .generationId(generation.getGenerationId())
                .modelTaskId(modelTaskId)
                .status("PROCESSING")
                .build();
    }

    /**
     * Step 2: 생성 상태 폴링 — model task → rig task → GLB 다운로드 자동 진행
     */
    @Transactional
    public GenerationStatusResponse pollStatus(Long siteId, Long generationId,
                                                CustomAdminDetails adminDetails) {
        validatePlatformAdmin(adminDetails);

        MascotGeneration gen = generationRepository.findById(generationId)
                .orElseThrow(() -> new CustomException(ErrorCode.MASCOT_GENERATION_NOT_FOUND));

        // 이미 완료 or 실패면 바로 반환
        if (gen.isFullyCompleted() || gen.isFailed()) {
            return GenerationStatusResponse.from(gen);
        }

        // Phase 1: model 생성 중
        if ("PROCESSING".equals(gen.getModelStatus())) {
            TripoApiService.TripoTaskStatus status = tripoApiService.getTaskStatus(gen.getModelTaskId());

            if (status.isFailed()) {
                gen.failModelGeneration("Tripo model 생성 실패");
                log.warn("마스코트 model 생성 실패: generationId={}", generationId);
                return GenerationStatusResponse.from(gen);
            }

            if (status.isSuccess()) {
                gen.completeModelGeneration();
                log.info("마스코트 model 생성 완료, rigging 시작: generationId={}", generationId);

                // 자동으로 rig task 시작
                String rigTaskId = tripoApiService.createAnimateRigTask(gen.getModelTaskId());
                gen.startRigging(rigTaskId);
            }

            // 아직 processing이면 그대로 반환
            return GenerationStatusResponse.from(gen);
        }

        // Phase 2: rigging 중
        if ("PROCESSING".equals(gen.getRigStatus())) {
            TripoApiService.TripoTaskStatus status = tripoApiService.getTaskStatus(gen.getRigTaskId());

            if (status.isFailed()) {
                gen.failRigging("Tripo rigging 실패");
                log.warn("마스코트 rigging 실패: generationId={}", generationId);
                return GenerationStatusResponse.from(gen);
            }

            if (status.isSuccess() && status.modelUrl() != null) {
                // GLB 다운로드 → 로컬 저장 → mascot 업데이트
                byte[] glbBytes = tripoApiService.downloadModel(status.modelUrl());
                String glbHash = FileValidator.computeFileHash(glbBytes);
                String modelUrl = fileStorageService.store(siteId, glbHash, glbBytes, "mascot.glb");

                gen.completeRigging(modelUrl);

                // tb_mascot에 model_url 자동 업데이트
                updateMascotModelUrl(siteId, modelUrl, gen);

                log.info("마스코트 3D 생성 완전 완료: generationId={}, modelUrl={}", generationId, modelUrl);
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

    private void updateMascotModelUrl(Long siteId, String modelUrl, MascotGeneration generation) {
        mascotRepository.findBySite_SiteId(siteId).ifPresent(mascot -> {
            mascot.updateModelUrl(modelUrl, "glb", generation);
            log.info("tb_mascot model_url 업데이트: siteId={}", siteId);
        });
    }

    private void validatePlatformAdmin(CustomAdminDetails adminDetails) {
        if (!AdminRole.PLATFORM_ADMIN.name().equals(adminDetails.getRole())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
    }
}
