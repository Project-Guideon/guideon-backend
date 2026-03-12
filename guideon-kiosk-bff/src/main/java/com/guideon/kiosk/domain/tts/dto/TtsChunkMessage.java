package com.guideon.kiosk.domain.tts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * TTS 문장 단위 응답 메시지 (Server → Unity)
 *
 * sentence-level streaming: 문장마다 개별 WS 메시지로 전송.
 * seq 번호로 순서 보장.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TtsChunkMessage {
    private String type;          // "chunk" 또는 "done"
    private int seq;              // 문장 순서 번호 (0부터)
    private String sentence;     // 원본 텍스트 문장
    private String audioBase64;  // Base64 인코딩 오디오
    private boolean last;        // 마지막 문장 여부
    private String sessionId;
}
