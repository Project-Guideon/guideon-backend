package com.guideon.kiosk.domain.stt.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guideon.kiosk.domain.stt.dto.SttResultMessage;
import com.guideon.kiosk.global.security.DeviceDetails;
import com.guideon.kiosk.global.security.DeviceTokenHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

/**
 * STT WebSocket Handler
 *
 * Unity → (binary audio chunk) → Kiosk BFF → FastAPI STT → (text JSON) → Unity
 *
 * 프로토콜:
 * - Client → Server: binary audio chunk (PCM/OGG)
 * - Server → Client: JSON { type: "partial"|"final", transcript, confidence, sessionId }
 *
 * FastAPI 미구현 시 stub 응답 반환.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SttWebSocketHandler extends BinaryWebSocketHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        DeviceDetails device = getDeviceDetails(session);
        String sessionId = getSessionId(session);
        log.info("STT WS 연결: deviceId={}, sessionId={}", device.getDeviceId(), sessionId);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        DeviceDetails device = getDeviceDetails(session);
        String sessionId = getSessionId(session);
        int audioSize = message.getPayloadLength();

        log.debug("STT 오디오 수신: deviceId={}, sessionId={}, size={}bytes",
                device.getDeviceId(), sessionId, audioSize);

        // TODO: FastAPI WS/REST로 오디오 전달 후 결과 수신
        // 현재는 stub 응답 반환
        SttResultMessage stubResult = SttResultMessage.builder()
                .type("final")
                .transcript("[STT 서비스 연결 대기 중]")
                .confidence(0.0)
                .sessionId(sessionId)
                .build();

        String json = objectMapper.writeValueAsString(stubResult);
        session.sendMessage(new TextMessage(json));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        DeviceDetails device = getDeviceDetails(session);
        log.error("STT WS 전송 오류: deviceId={}, error={}",
                device != null ? device.getDeviceId() : "unknown", exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        DeviceDetails device = getDeviceDetails(session);
        log.info("STT WS 종료: deviceId={}, status={}",
                device != null ? device.getDeviceId() : "unknown", status);
    }

    private DeviceDetails getDeviceDetails(WebSocketSession session) {
        return (DeviceDetails) session.getAttributes()
                .get(DeviceTokenHandshakeInterceptor.DEVICE_DETAILS_ATTR);
    }

    private String getSessionId(WebSocketSession session) {
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if ("sessionId".equals(kv[0]) && kv.length == 2) {
                    return kv[1];
                }
            }
        }
        return session.getId();
    }
}
