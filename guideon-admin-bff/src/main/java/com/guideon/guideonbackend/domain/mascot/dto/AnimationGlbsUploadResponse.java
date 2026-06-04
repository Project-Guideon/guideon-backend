package com.guideon.guideonbackend.domain.mascot.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** POST /mascot/animations 응답 */
@Getter
@Builder
public class AnimationGlbsUploadResponse {

    private List<UploadedClip> uploaded;

    /** 병합 완료 시 animModelUrl, 마스코트 미생성 또는 병합 실패 시 null */
    private String animModelUrl;

    @Getter
    @Builder
    public static class UploadedClip {
        private String stateKey;  // idle | speaking | listening | thinking | greeting
        private String clipName;  // Idle | Talking | Listening | Thinking | Waving
        private String glbUrl;    // 저장된 GLB URL
    }
}
