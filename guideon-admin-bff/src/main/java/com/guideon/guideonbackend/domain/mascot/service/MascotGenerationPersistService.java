package com.guideon.guideonbackend.domain.mascot.service;

import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import com.guideon.core.domain.mascot.entity.MascotGeneration;
import com.guideon.core.domain.mascot.repository.MascotGenerationRepository;
import com.guideon.core.domain.mascot.repository.MascotRepository;
import com.guideon.core.domain.site.entity.Site;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * MascotGeneration DB 저장/업데이트 전담.
 * 외부 API 호출 없이 트랜잭션 범위를 최소화.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MascotGenerationPersistService {

    private final MascotGenerationRepository generationRepository;
    private final MascotRepository mascotRepository;

    @Transactional
    public MascotGeneration saveNewGeneration(Site site, String sourceImageUrl, String modelTaskId) {
        MascotGeneration generation = MascotGeneration.builder()
                .site(site)
                .sourceImageUrl(sourceImageUrl)
                .build();
        generation.startModelGeneration(modelTaskId);
        return generationRepository.save(generation);
    }

    @Transactional(readOnly = true)
    public MascotGeneration loadAndValidate(Long siteId, Long generationId) {
        MascotGeneration gen = generationRepository.findById(generationId)
                .orElseThrow(() -> new CustomException(ErrorCode.MASCOT_GENERATION_NOT_FOUND));

        if (!gen.getSite().getSiteId().equals(siteId)) {
            throw new CustomException(ErrorCode.MASCOT_GENERATION_NOT_FOUND);
        }
        return gen;
    }

    @Transactional
    public MascotGeneration applyModelComplete(Long generationId, String rigTaskId) {
        MascotGeneration gen = generationRepository.findById(generationId)
                .orElseThrow(() -> new CustomException(ErrorCode.MASCOT_GENERATION_NOT_FOUND));
        gen.completeModelGeneration();
        gen.startRigging(rigTaskId);
        return gen;
    }

    @Transactional
    public MascotGeneration applyModelFailed(Long generationId, String reason) {
        MascotGeneration gen = generationRepository.findById(generationId)
                .orElseThrow(() -> new CustomException(ErrorCode.MASCOT_GENERATION_NOT_FOUND));
        gen.failModelGeneration(reason);
        return gen;
    }

    @Transactional
    public MascotGeneration applyRigComplete(Long siteId, Long generationId, String modelUrl) {
        MascotGeneration gen = generationRepository.findById(generationId)
                .orElseThrow(() -> new CustomException(ErrorCode.MASCOT_GENERATION_NOT_FOUND));
        gen.completeRigging(modelUrl);

        // tb_mascot에 base model_url 업데이트 (재생성 시 옛 anim 메타데이터 먼저 초기화)
        mascotRepository.findBySite_SiteId(siteId).ifPresentOrElse(
                mascot -> {
                    mascot.clearAnimation();
                    mascot.updateModelUrl(modelUrl, "glb", gen);
                    log.info("tb_mascot model_url 업데이트: siteId={}", siteId);
                },
                () -> log.warn("tb_mascot 미존재 — model_url 업데이트 생략: siteId={}, modelUrl={}, generationId={}",
                        siteId, modelUrl, generationId)
        );
        return gen;
    }

    @Transactional
    public MascotGeneration applyRigFailed(Long generationId, String reason) {
        MascotGeneration gen = generationRepository.findById(generationId)
                .orElseThrow(() -> new CustomException(ErrorCode.MASCOT_GENERATION_NOT_FOUND));
        gen.failRigging(reason);
        return gen;
    }

    /**
     * mesh-processor 병합 완료: tb_mascot.animModelUrl + animClips 반영.
     * rig 완료 후 별도 트랜잭션으로 실행 (외부 호출 결과).
     */
    /**
     * clean mesh FBX URL을 tb_mascot에 직접 저장.
     * Tripo pre-rig 결과가 FBX인 경우(GLB→FBX 변환 불필요) 사용.
     */
    @Transactional
    public void saveCleanMeshUrl(Long siteId, String cleanMeshUrl) {
        mascotRepository.findBySite_SiteId(siteId).ifPresent(
                mascot -> mascot.updateCleanMeshUrl(cleanMeshUrl)
        );
    }

    @Transactional
    public void applyAnimationComplete(Long siteId, Long generationId,
                                       String animModelUrl, Map<String, String> animClips) {
        mascotRepository.findBySite_SiteId(siteId).ifPresentOrElse(
                mascot -> {
                    mascot.updateAnimation(animModelUrl, animClips);
                    log.info("tb_mascot animModelUrl 자동 업데이트: siteId={}, clips={}",
                            siteId, animClips.keySet());
                },
                () -> log.warn("tb_mascot 미존재 — animModelUrl 업데이트 생략: siteId={}, generationId={}",
                        siteId, generationId)
        );
    }

}
