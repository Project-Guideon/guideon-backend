package com.guideon.guideonbackend.domain.mascot.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/** GET /mascot/anim-config 응답 — 현재 상태→클립명 매핑 */
@Getter
@Builder
public class AnimConfigResponse {

    /** 상태→클립명 (예: {idle:"Idle", speaking:"Talking", ...}) */
    private Map<String, String> animClips;

    /** 상태→GLB URL (업로드된 원본 GLB 경로) */
    private Map<String, String> animGlbUrls;
}
