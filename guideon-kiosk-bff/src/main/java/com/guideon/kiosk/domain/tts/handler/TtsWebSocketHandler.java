package com.guideon.kiosk.domain.tts.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guideon.kiosk.domain.tts.dto.TtsChunkMessage;
import com.guideon.kiosk.domain.tts.dto.TtsRequestMessage;
import com.guideon.kiosk.global.security.DeviceDetails;
import com.guideon.kiosk.global.security.DeviceTokenHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * TTS WebSocket Handler
 *
 * Unity → (text JSON) → Kiosk BFF → FastAPI TTS → (audio chunk JSON) → Unity
 *
 * 프로토콜:
 * - Client → Server: JSON { sessionId, text, language, voiceId }
 * - Server → Client: JSON { type: "chunk"|"done", seq, sentence, audioBase64, last, sessionId }
 *
 * sentence-level streaming: 문장 경계(마침표/물음표/느낌표)마다 개별 응답.
 * seq 번호로 순서 보장.
 *
 * FastAPI 미구현 시 stub 응답 반환.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TtsWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        DeviceDetails device = getDeviceDetails(session);
        log.info("TTS WS 연결: deviceId={}", device.getDeviceId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        DeviceDetails device = getDeviceDetails(session);
        TtsRequestMessage request = objectMapper.readValue(message.getPayload(), TtsRequestMessage.class);

        log.debug("TTS 요청: deviceId={}, sessionId={}, textLen={}",
                device.getDeviceId(), request.getSessionId(),
                request.getText() != null ? request.getText().length() : 0);

        // TODO: FastAPI /internal/v1/tts/synthesize 호출 후 sentence-level 스트리밍
        // 현재는 stub: 문장 분리 후 더미 오디오 전송
        String[] sentences = splitSentences(request.getText());

        for (int i = 0; i < sentences.length; i++) {
            TtsChunkMessage chunk = TtsChunkMessage.builder()
                    .type("chunk")
                    .seq(i)
                    .sentence(sentences[i])
                    .audioBase64("")  // stub: 빈 오디오
                    .last(i == sentences.length - 1)
                    .sessionId(request.getSessionId())
                    .build();

            String json = objectMapper.writeValueAsString(chunk);
            session.sendMessage(new TextMessage(json));
        }

        // 완료 메시지
        TtsChunkMessage done = TtsChunkMessage.builder()
                .type("done")
                .seq(sentences.length)
                .sentence(null)
                .audioBase64(null)
                .last(true)
                .sessionId(request.getSessionId())
                .build();

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(done)));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        DeviceDetails device = getDeviceDetails(session);
        log.error("TTS WS 전송 오류: deviceId={}, error={}",
                device != null ? device.getDeviceId() : "unknown", exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        DeviceDetails device = getDeviceDetails(session);
        log.info("TTS WS 종료: deviceId={}, status={}",
                device != null ? device.getDeviceId() : "unknown", status);
    }

    /**
     * 문장 경계(마침표/물음표/느낌표)로 텍스트 분리
     */
    private String[] splitSentences(String text) {
        if (text == null || text.isBlank()) {
            return new String[]{"[TTS 서비스 연결 대기 중]"};
        }
        // 마침표/물음표/느낌표 뒤에서 분리, 빈 문자열 제거
        String[] raw = text.split("(?<=[.?!。？！])\\s*");
        if (raw.length == 0) {
            return new String[]{text};
        }
        return raw;
    }

    private DeviceDetails getDeviceDetails(WebSocketSession session) {
        return (DeviceDetails) session.getAttributes()
                .get(DeviceTokenHandshakeInterceptor.DEVICE_DETAILS_ATTR);
    }
}
