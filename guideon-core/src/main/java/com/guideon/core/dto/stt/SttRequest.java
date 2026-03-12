package com.guideon.core.dto.stt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FastAPI STT 요청 DTO
 *
 * Kiosk BFF → FastAPI /internal/v1/stt/recognize
 * 오디오 바이너리는 별도 전송, 여기는 메타데이터만.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SttRequest {
    private String sessionId;
    private String language;   // "ko", "en", "ja" 등
    private String encoding;   // "LINEAR16", "OGG_OPUS" 등
    private int sampleRate;    // 16000
}
