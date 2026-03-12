package com.guideon.kiosk.domain.stt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * STT 결과 메시지 (Server → Unity)
 *
 * WS로 Unity에 전송되는 JSON 메시지.
 * isFinal=true 이면 최종 인식 결과.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SttResultMessage {
    private String type;          // "partial" 또는 "final"
    private String transcript;
    private double confidence;
    private String sessionId;
}
