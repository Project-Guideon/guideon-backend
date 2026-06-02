package com.guideon.guideonbackend.domain.mascot.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 키오스크 상태 ↔ Tripo animate_retarget 프리셋 모션 매핑.
 *
 * Tripo rig 기본 버전: v1.0-20240301 (biped) → 프리셋 형식: preset:biped:*
 *
 * Unity는 animClips 맵의 key(state)로 상태를 식별하고, value(preset)로 클립 이름을 찾아 재생.
 */
public enum MascotMotion {

    IDLE      ("idle",      "preset:biped:idle"),
    SPEAKING  ("speaking",  "preset:biped:sing_01"),   // 입·상체 제스처 (실제 TTS 립싱크는 jaw bone)
    LISTENING ("listening", "preset:biped:wait"),
    THINKING  ("thinking",  "preset:biped:scratch"),   // 머리 긁적이는 고민 포즈
    GREETING  ("greeting",  "preset:biped:greet_01");

    private final String state;        // animClips 맵 key / Unity 상태 식별자
    private final String tripoPreset;  // Tripo animations 배열에 넘기는 값 / GLB 내부 클립명

    MascotMotion(String state, String tripoPreset) {
        this.state = state;
        this.tripoPreset = tripoPreset;
    }

    public String getState()       { return state; }
    public String getTripoPreset() { return tripoPreset; }

    /** Tripo animations 배열용 preset 리스트 (순서 유지). */
    public static List<String> presetList() {
        return Arrays.stream(values())
                .map(MascotMotion::getTripoPreset)
                .collect(Collectors.toList());
    }

    /** Unity에 내려줄 animClips 맵 (state → tripoPreset = GLB 내부 클립명). */
    public static Map<String, String> clipMap() {
        return Arrays.stream(values())
                .collect(Collectors.toMap(MascotMotion::getState, MascotMotion::getTripoPreset));
    }
}
