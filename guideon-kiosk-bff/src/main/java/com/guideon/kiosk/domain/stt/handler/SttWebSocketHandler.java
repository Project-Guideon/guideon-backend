package com.guideon.kiosk.domain.stt.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guideon.kiosk.global.config.FastApiConfig;
import com.guideon.kiosk.global.security.DeviceDetails;
import com.guideon.kiosk.global.security.DeviceTokenHandshakeInterceptor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * STT WebSocket Handler
 *
 * Unity → (binary PCM) / (text control) → BFF → FastAPI /ws/stream
 *                                                         ↓
 * Unity ← (stt_interim/stt_final/tts_chunk/final_text/done) ← BFF ← FastAPI
 *
 * 프로토콜 (Unity → BFF):
 * - binary frame: PCM 오디오 청크
 * - text frame:   {"type":"stop"} — 오디오 스트림 종료 신호
 *
 * 프로토콜 (BFF → Unity, FastAPI 메시지 그대로 중계):
 * - {"type":"status",  "stage":"stt_start"|"stt_done"|"tts_start"|...}
 * - {"type":"stt_interim", "text":"...", "language_code":"...", "confidence":0.9}
 * - {"type":"stt_final",   "text":"...", "language_code":"...", "confidence":0.9}
 * - {"type":"tts_chunk",   "seq":0, "text":"...", "audio_b64":"...", "is_final":true}
 * - {"type":"final_text",  "query":"...", "answer":"..."}
 * - {"type":"done"}
 * - {"type":"error",   "code":"...", "message":"..."}
 *
 * WS 쿼리 파라미터 (Unity → BFF):
 * - sessionId:    채팅 세션 ID (필수)
 * - siteId:       사이트 ID (기본값 1)
 * - languageCode: 언어 코드 (기본값 ko-KR)
 * - sampleRate:   PCM 샘플레이트 Hz (기본값 16000)
 * - ttsStream:    TTS 스트리밍 여부 (기본값 true)
 */
@Slf4j
@Component
public class SttWebSocketHandler extends AbstractWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final FastApiConfig fastApiConfig;
    private final OkHttpClient okHttpClient;

    /** Spring WS session.getId() → FastApiStreamSession */
    private final ConcurrentHashMap<String, FastApiStreamSession> sessions = new ConcurrentHashMap<>();

    public SttWebSocketHandler(
            ObjectMapper objectMapper,
            FastApiConfig fastApiConfig,
            @Qualifier("fastapiOkHttpClient") OkHttpClient okHttpClient
    ) {
        this.objectMapper = objectMapper;
        this.fastApiConfig = fastApiConfig;
        this.okHttpClient = okHttpClient;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        DeviceDetails device = getDeviceDetails(session);
        String sessionId = getSessionId(session);
        log.info("STT WS 연결: deviceId={}, sessionId={}", device.getDeviceId(), sessionId);

        Map<String, String> params = parseQueryParams(session);
        int siteId = parseIntOrDefault(params.get("siteId"), 1);
        String languageCode = params.getOrDefault("languageCode", "ko-KR");
        int sampleRate = parseIntOrDefault(params.get("sampleRate"), 16000);
        boolean ttsStream = !"false".equalsIgnoreCase(params.get("ttsStream"));

        String startPayload = buildStartPayload(siteId, languageCode, sampleRate, ttsStream);

        FastApiStreamSession fastApiSession = new FastApiStreamSession(
                okHttpClient,
                fastApiConfig.getWsStreamUrl(),
                session,
                sessionId,
                startPayload
        );
        sessions.put(session.getId(), fastApiSession);
    }

    /** Unity → binary PCM → FastAPI */
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        FastApiStreamSession fastApiSession = sessions.get(session.getId());
        if (fastApiSession != null) {
            fastApiSession.relayBinary(message.getPayload().array());
        }
    }

    /** Unity → text({"type":"stop"}) → FastAPI */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        FastApiStreamSession fastApiSession = sessions.get(session.getId());
        if (fastApiSession != null) {
            fastApiSession.relayText(message.getPayload());
        }
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

        FastApiStreamSession fastApiSession = sessions.remove(session.getId());
        if (fastApiSession != null) {
            fastApiSession.close();
        }
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private String buildStartPayload(int siteId, String languageCode, int sampleRate, boolean ttsStream) {
        try {
            Map<String, Object> start = new HashMap<>();
            start.put("type", "start");
            start.put("site_id", siteId);
            start.put("language_code", languageCode);
            start.put("sample_rate_hz", sampleRate);
            start.put("interim_results", true);
            start.put("tts_stream", ttsStream);
            start.put("realtime", true);
            return objectMapper.writeValueAsString(start);
        } catch (Exception e) {
            return String.format(
                    "{\"type\":\"start\",\"site_id\":%d,\"language_code\":\"%s\","
                            + "\"sample_rate_hz\":%d,\"interim_results\":true,"
                            + "\"tts_stream\":%b,\"realtime\":true}",
                    siteId, languageCode, sampleRate, ttsStream
            );
        }
    }

    private Map<String, String> parseQueryParams(WebSocketSession session) {
        Map<String, String> params = new HashMap<>();
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query == null) return params;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                params.put(
                        URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(kv[1], StandardCharsets.UTF_8)
                );
            }
        }
        return params;
    }

    private String getSessionId(WebSocketSession session) {
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if ("sessionId".equals(kv[0]) && kv.length == 2) {
                    return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                }
            }
        }
        return session.getId();
    }

    private DeviceDetails getDeviceDetails(WebSocketSession session) {
        return (DeviceDetails) session.getAttributes()
                .get(DeviceTokenHandshakeInterceptor.DEVICE_DETAILS_ATTR);
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
