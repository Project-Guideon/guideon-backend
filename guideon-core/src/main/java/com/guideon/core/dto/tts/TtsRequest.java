package com.guideon.core.dto.tts;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FastAPI TTS 요청 DTO
 *
 * Kiosk BFF → FastAPI /internal/v1/tts/synthesize
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TtsRequest {
    private String sessionId;
    private String text;
    private String language;     // "ko", "en", "ja"
    private String voiceId;      // 마스코트별 음성 ID
}
