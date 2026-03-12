package com.guideon.kiosk.domain.tts.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * TTS 요청 메시지 (Unity → Server)
 *
 * Unity가 WS로 보내는 JSON 메시지.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TtsRequestMessage {
    private String sessionId;
    private String text;
    private String language;
    private String voiceId;
}
