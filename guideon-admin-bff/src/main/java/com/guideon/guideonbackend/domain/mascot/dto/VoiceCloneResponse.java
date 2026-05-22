package com.guideon.guideonbackend.domain.mascot.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VoiceCloneResponse {

    /** Cartesia에서 생성된 커스텀 보이스 ID. tb_mascot.tts_voice_id에 저장됩니다. */
    private String voiceId;

    /** 보이스 이름 (Cartesia에서 반환된 값) */
    private String name;
}
