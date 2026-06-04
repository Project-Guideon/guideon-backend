package com.guideon.guideonbackend.domain.mascot.service;

import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import com.guideon.core.domain.admin.entity.AdminRole;
import com.guideon.core.domain.mascot.entity.MascotAnimConfig;
import com.guideon.core.domain.mascot.entity.MascotGeneration;
import com.guideon.core.domain.mascot.repository.MascotAnimConfigRepository;
import com.guideon.core.domain.mascot.repository.MascotGenerationRepository;
import com.guideon.guideonbackend.domain.mascot.dto.AnimConfigRequest;
import com.guideon.guideonbackend.domain.mascot.dto.AnimConfigResponse;
import com.guideon.guideonbackend.domain.mascot.dto.AnimationGlbsUploadResponse;
import com.guideon.guideonbackend.global.security.CustomAdminDetails;
import com.guideon.guideonbackend.global.storage.FileStorageService;
import com.guideon.guideonbackend.global.storage.FileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 마스코트 애니메이션 설정(tb_mascot_anim_config) 관리.
 * 사이트별 state→clip GLB를 1회 설정하면, 이후 마스코트 생성 시 자동으로 anim GLB가 병합된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MascotAnimConfigService {

    /** state_key → 기본 clip_name 매핑 (담당자가 설정하지 않은 경우 사용) */
    private static final Map<String, String> DEFAULT_CLIP_NAMES = Map.of(
            "idle",      "Idle",
            "speaking",  "Talking",
            "listening", "Listening",
            "thinking",  "Thinking",
            "greeting",  "Waving"
    );

    private final MascotAnimConfigRepository animConfigRepository;
    private final MascotGenerationRepository generationRepository;
    private final FileStorageService fileStorageService;
    private final MascotAnimationMergeService animationMergeService;

    /**
     * state별 GLB 파일 업로드.
     * 각 state에 대해 기존 레코드가 있으면 업데이트, 없으면 신규 생성(upsert).
     *
     * @param files state_key → MultipartFile 맵 (전달된 state만 처리)
     */
    @Transactional
    public AnimationGlbsUploadResponse uploadAnimationGlbs(
            Long siteId,
            Map<String, MultipartFile> files,
            CustomAdminDetails adminDetails) {

        validatePlatformAdmin(adminDetails);

        if (files == null || files.isEmpty()) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "업로드할 GLB 파일이 없습니다.");
        }

        List<AnimationGlbsUploadResponse.UploadedClip> uploaded = new ArrayList<>();

        for (Map.Entry<String, MultipartFile> entry : files.entrySet()) {
            String stateKey = entry.getKey();
            MultipartFile file = entry.getValue();

            if (file == null || file.isEmpty()) continue;

            if (!DEFAULT_CLIP_NAMES.containsKey(stateKey)) {
                throw new CustomException(ErrorCode.VALIDATION_ERROR,
                        "유효하지 않은 state_key: " + stateKey
                        + ". 허용값: " + DEFAULT_CLIP_NAMES.keySet());
            }

            FileValidator.validateGlb(file);
            String fileHash = FileValidator.computeFileHash(file);
            String glbUrl   = fileStorageService.store(siteId, fileHash, file);

            // 기존 레코드 upsert
            MascotAnimConfig config = animConfigRepository
                    .findBySiteIdAndStateKey(siteId, stateKey)
                    .orElse(null);

            String clipName = DEFAULT_CLIP_NAMES.get(stateKey);

            if (config == null) {
                config = MascotAnimConfig.builder()
                        .siteId(siteId)
                        .stateKey(stateKey)
                        .clipName(clipName)
                        .glbUrl(glbUrl)
                        .build();
                animConfigRepository.save(config);
            } else {
                config.update(config.getClipName(), glbUrl); // 클립명은 유지, GLB만 교체
            }

            log.info("anim GLB 업로드: siteId={}, state={}, url={}", siteId, stateKey, glbUrl);
            uploaded.add(AnimationGlbsUploadResponse.UploadedClip.builder()
                    .stateKey(stateKey)
                    .clipName(config.getClipName())
                    .glbUrl(glbUrl)
                    .build());
        }

        // rig가 완료된 마스코트가 있으면 즉시 병합 — animModelUrl을 응답에 반영
        String animModelUrl = triggerMergeIfAvailable(siteId);

        return AnimationGlbsUploadResponse.builder()
                .uploaded(uploaded)
                .animModelUrl(animModelUrl)
                .build();
    }

    /**
     * 현재 anim config 조회.
     */
    @Transactional(readOnly = true)
    public AnimConfigResponse getAnimConfig(Long siteId, CustomAdminDetails adminDetails) {
        validatePlatformAdmin(adminDetails);

        List<MascotAnimConfig> configs = animConfigRepository.findBySiteId(siteId);

        Map<String, String> animClips = configs.stream()
                .collect(Collectors.toMap(MascotAnimConfig::getStateKey, MascotAnimConfig::getClipName));
        Map<String, String> animGlbUrls = configs.stream()
                .collect(Collectors.toMap(MascotAnimConfig::getStateKey, MascotAnimConfig::getGlbUrl));

        return AnimConfigResponse.builder()
                .animClips(animClips)
                .animGlbUrls(animGlbUrls)
                .build();
    }

    /**
     * 클립명만 수정 (GLB 파일 변경 없음).
     * 전달된 state_key만 업데이트.
     */
    @Transactional
    public AnimConfigResponse updateAnimConfig(Long siteId, AnimConfigRequest request,
                                               CustomAdminDetails adminDetails) {
        validatePlatformAdmin(adminDetails);

        if (request.getAnimClips() == null || request.getAnimClips().isEmpty()) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "animClips가 비어있습니다.");
        }

        for (Map.Entry<String, String> entry : request.getAnimClips().entrySet()) {
            String stateKey  = entry.getKey();
            String clipName  = entry.getValue();

            MascotAnimConfig config = animConfigRepository
                    .findBySiteIdAndStateKey(siteId, stateKey)
                    .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND,
                            "anim config 없음 — GLB를 먼저 업로드해주세요: state=" + stateKey));

            config.updateClipName(clipName);
            log.info("anim 클립명 수정: siteId={}, state={}, clipName={}", siteId, stateKey, clipName);
        }

        return getAnimConfig(siteId, adminDetails);
    }

    /**
     * 해당 site의 최신 rig 완료 이력이 있으면 병합을 동기 실행하고 animModelUrl 반환.
     * 없으면 null 반환 (이후 rig 완료 시점에 자동 병합됨).
     */
    private String triggerMergeIfAvailable(Long siteId) {
        return generationRepository.findTopBySite_SiteIdOrderByCreatedAtDesc(siteId)
                .filter(MascotGeneration::isFullyCompleted)
                .filter(gen -> gen.getResultModelUrl() != null)
                .map(gen -> {
                    animationMergeService.mergeIfRiggedMascotExists(
                            siteId, gen.getGenerationId(), gen.getResultModelUrl());
                    return fileStorageService.resolveUrl(siteId, "anim_" + gen.getGenerationId() + ".glb");
                })
                .orElse(null);
    }

    private void validatePlatformAdmin(CustomAdminDetails adminDetails) {
        if (!AdminRole.PLATFORM_ADMIN.name().equals(adminDetails.getRole())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
    }
}
