package com.guideon.core.dto.tts;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FastAPI TTS 응답 DTO (문장 단위)
 *
 * FastAPI → Kiosk BFF → Unity
 * sentence-level streaming: 문장 경계마다 별도 응답
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TtsResponse {
    private int seq;              // 문장 순서 번호 (0부터)
    private String sentence;     // 원본 텍스트 문장
    private String audioBase64;  // Base64 인코딩된 오디오 데이터
    private boolean isLast;      // 마지막 문장 여부
}
