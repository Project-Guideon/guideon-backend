package com.guideon.guideonbackend.domain.mascot.service;

import com.guideon.core.domain.mascot.entity.MascotAnimConfig;
import com.guideon.core.domain.mascot.repository.MascotAnimConfigRepository;
import com.guideon.guideonbackend.global.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * rig 완료된 마스코트 GLB + 상태별 Mixamo 애니메이션 GLB → 통합 anim GLB 병합.
 *
 * rig 완료 시점(MascotGenerationService)과
 * 애니메이션 GLB 업로드 시점(MascotAnimConfigService) 양쪽에서 공통으로 호출된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MascotAnimationMergeService {

    private final MascotAnimConfigRepository animConfigRepository;
    private final FileStorageService fileStorageService;
    private final MeshProcessorClient meshProcessorClient;
    private final MascotGenerationPersistService persistService;

    /**
     * anim_config가 설정돼 있고 rig GLB가 존재하면 mesh-processor에 병합을 요청한다.
     *
     * @param siteId          사이트 ID
     * @param generationId    MascotGeneration PK (출력 파일명 + DB 업데이트에 사용)
     * @param riggedModelUrl  Tripo rig 결과 GLB 서빙 URL
     * @return 병합 성공 시 animModelUrl, anim_config 미설정 또는 병합 실패 시 null
     */
    public String mergeIfRiggedMascotExists(Long siteId, Long generationId, String riggedModelUrl) {
        List<MascotAnimConfig> animConfigs = animConfigRepository.findBySiteId(siteId);
        if (animConfigs.isEmpty()) {
            log.info("anim_config 미설정 — anim 병합 skip: siteId={}", siteId);
            return null;
        }

        try {
            // clipName이 중복일 때 IllegalStateException 방지 — 첫 번째 값 유지 (a, b) -> a
            Map<String, String> animGlbs = animConfigs.stream().collect(Collectors.toMap(
                    MascotAnimConfig::getClipName,
                    c -> fileStorageService.toLocalPath(c.getGlbUrl()).toString(),
                    (a, b) -> a
            ));

            String riggedLocalPath = fileStorageService.toLocalPath(riggedModelUrl).toString();
            String animFilename    = "anim_" + generationId + ".glb";
            String animModelUrl    = fileStorageService.resolveUrl(siteId, animFilename);
            String animLocalPath   = fileStorageService.toLocalPath(animModelUrl).toString();

            meshProcessorClient.combine(riggedLocalPath, animGlbs, animLocalPath);

            Map<String, String> animClips = animConfigs.stream().collect(Collectors.toMap(
                    MascotAnimConfig::getStateKey,
                    MascotAnimConfig::getClipName
            ));
            persistService.applyAnimationComplete(siteId, generationId, animModelUrl, animClips);
            log.info("anim GLB 병합 완료: generationId={}, animModelUrl={}", generationId, animModelUrl);
            return animModelUrl;

        } catch (Exception e) {
            log.warn("mesh-processor 실패 — animModelUrl 없이 완료: generationId={}, err={}",
                    generationId, e.getMessage());
            return null;
        }
    }
}
