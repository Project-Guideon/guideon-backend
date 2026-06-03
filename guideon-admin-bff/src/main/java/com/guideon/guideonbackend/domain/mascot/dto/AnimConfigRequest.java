package com.guideon.guideonbackend.domain.mascot.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/** PUT /mascot/anim-config 요청 — 클립명 매핑만 수정 (GLB 파일 변경 없음) */
@Getter
@NoArgsConstructor
public class AnimConfigRequest {

    /** 수정할 상태→클립명 맵. 전달된 state_key만 업데이트됨. */
    private Map<String, String> animClips;
}
