package com.guideon.guideonbackend.domain.mascot.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** POST /mascot/animations 응답 */
@Getter
@Builder
public class AnimationGlbsUploadResponse {

    private List<UploadedClip> uploaded;

    @Getter
    @Builder
    public static class UploadedClip {
        private String stateKey;  // idle | speaking | listening | thinking | greeting
        private String clipName;  // Idle | Talking | Listening | Thinking | Waving
        private String glbUrl;    // 저장된 GLB URL
    }
}
