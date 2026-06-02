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

        // tb_mascot에 base model_url 자동 업데이트
        mascotRepository.findBySite_SiteId(siteId).ifPresentOrElse(
                mascot -> {
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

    /** retarget task 1개 생성 후 PROCESSING 전환. */
    @Transactional
    public MascotGeneration applyRetargetStarted(Long generationId, String retargetTaskId) {
        MascotGeneration gen = generationRepository.findById(generationId)
                .orElseThrow(() -> new CustomException(ErrorCode.MASCOT_GENERATION_NOT_FOUND));
        gen.startRetarget(retargetTaskId);
        return gen;
    }

    /**
     * retarget GLB 다운로드 완료: generation.animModelUrl 저장 + tb_mascot.animModelUrl/animClips 반영.
     *
     * @param animClips MascotMotion.clipMap() — 상태→클립명 (Unity용)
     */
    @Transactional
    public MascotGeneration applyRetargetComplete(Long siteId, Long generationId,
                                                   String animModelUrl, Map<String, String> animClips) {
        MascotGeneration gen = generationRepository.findById(generationId)
                .orElseThrow(() -> new CustomException(ErrorCode.MASCOT_GENERATION_NOT_FOUND));
        gen.completeRetarget(animModelUrl);

        mascotRepository.findBySite_SiteId(siteId).ifPresentOrElse(
                mascot -> {
                    mascot.updateAnimation(animModelUrl, animClips);
                    log.info("tb_mascot animModelUrl 업데이트: siteId={}, animClips={}", siteId, animClips.keySet());
                },
                () -> log.warn("tb_mascot 미존재 — animModelUrl 업데이트 생략: siteId={}, generationId={}",
                        siteId, generationId)
        );
        return gen;
    }

    /** retarget 실패 — generation을 FAILED로 마킹하지 않음(base GLB는 확보됨). */
    @Transactional
    public MascotGeneration applyRetargetFailed(Long generationId, String reason) {
        MascotGeneration gen = generationRepository.findById(generationId)
                .orElseThrow(() -> new CustomException(ErrorCode.MASCOT_GENERATION_NOT_FOUND));
        gen.failRetarget(reason);
        log.warn("retarget 실패(base GLB 유지): generationId={}, reason={}", generationId, reason);
        return gen;
    }
}
