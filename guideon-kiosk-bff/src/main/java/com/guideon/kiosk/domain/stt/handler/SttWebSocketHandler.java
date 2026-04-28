package com.guideon.kiosk.domain.stt.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guideon.core.dto.dailyinfo.DailyInfoDto;
import com.guideon.core.dto.chat.WsChatSaveCommand;
import com.guideon.core.dto.kiosk.KioskMascotDto;
import com.guideon.kiosk.client.CoreChatClient;
import com.guideon.kiosk.client.CoreDailyInfoClient;
import com.guideon.kiosk.client.CoreKioskClient;
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
import java.util.List;
import java.util.Locale;
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
 * - {"type":"final_text",  "query":"...", "answer":"...", "category":"..."}
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
    private final CoreKioskClient coreKioskClient;
    private final CoreChatClient coreChatClient;
    private final CoreDailyInfoClient coreDailyInfoClient;

    /** Spring WS session.getId() → FastApiStreamSession */
    private final ConcurrentHashMap<String, FastApiStreamSession> sessions = new ConcurrentHashMap<>();

    public SttWebSocketHandler(
            ObjectMapper objectMapper,
            FastApiConfig fastApiConfig,
            @Qualifier("fastapiOkHttpClient") OkHttpClient okHttpClient,
            CoreKioskClient coreKioskClient,
            CoreChatClient coreChatClient,
            CoreDailyInfoClient coreDailyInfoClient
    ) {
        this.objectMapper = objectMapper;
        this.fastApiConfig = fastApiConfig;
        this.okHttpClient = okHttpClient;
        this.coreKioskClient = coreKioskClient;
        this.coreChatClient = coreChatClient;
        this.coreDailyInfoClient = coreDailyInfoClient;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        DeviceDetails device = getDeviceDetails(session);
        if (device == null || device.getSiteId() == null || device.getDeviceId() == null) {
            log.warn("STT WS connection rejected: missing authenticated device context");
            session.close(CloseStatus.POLICY_VIOLATION.withReason("device authentication is required"));
            return;
        }

        Map<String, String> params = parseQueryParams(session);
        String sessionId = params.get("sessionId");
        if (sessionId == null || sessionId.isBlank()) {
            log.warn("STT WS 연결 거부: sessionId 누락");
            session.close(CloseStatus.BAD_DATA.withReason("sessionId is required"));
            return;
        }

        log.info("STT WS 연결: deviceId={}, sessionId={}",
                device.getDeviceId(), sessionId);

        Long siteId = device.getSiteId();
        String languageCode = params.getOrDefault("languageCode", "ko-KR");
        int sampleRate = parsePositiveIntOrDefault(params.get("sampleRate"), 16000);
        boolean ttsStream = !"false".equalsIgnoreCase(params.get("ttsStream"));

        String requestedSiteId = params.get("siteId");
        if (requestedSiteId != null && !requestedSiteId.equals(String.valueOf(siteId))) {
            log.warn("STT WS siteId query ignored: deviceId={}, requestedSiteId={}, authenticatedSiteId={}",
                    device.getDeviceId(), requestedSiteId, siteId);
        }

        String deviceId = device.getDeviceId();
        KioskMascotDto mascot = fetchMascot(deviceId);
        List<DailyInfoDto> dailyInfos = fetchDailyInfos(siteId);
        Map<String, Object> deviceLocation = buildDeviceLocationPayload(device);
        String startPayload = buildStartPayload(
                sessionId, siteId, deviceId, languageCode, sampleRate, ttsStream, mascot, dailyInfos, deviceLocation);

        DeviceDetails deviceForCallback = device;
        FastApiStreamSession fastApiSession = new FastApiStreamSession(
                okHttpClient,
                fastApiConfig.getWsStreamUrl(),
                session,
                sessionId,
                startPayload,
                (query, answer, category) -> saveWsChatHistory(sessionId, deviceForCallback, siteId, languageCode, query, answer, category)
        );
        sessions.put(session.getId(), fastApiSession);
    }

    /** Unity → binary PCM → FastAPI */
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        FastApiStreamSession fastApiSession = sessions.get(session.getId());
        if (fastApiSession != null) {
            java.nio.ByteBuffer payload = message.getPayload().asReadOnlyBuffer();
            byte[] bytes = new byte[payload.remaining()];
            payload.get(bytes);
            fastApiSession.relayBinary(bytes);
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

    /** deviceId로 Core에서 마스코트 정보 조회. 실패 시 null 반환 → FastAPI 기본 프롬프트 사용 */
    private KioskMascotDto fetchMascot(String deviceId) {
        if (deviceId == null) return null;
        try {
            return coreKioskClient.getMascot(deviceId);
        } catch (Exception e) {
            log.warn("[SttWS] mascot 조회 실패 (기본 프롬프트 사용): deviceId={}, error={}", deviceId, e.getMessage());
            return null;
        }
    }

    private List<DailyInfoDto> fetchDailyInfos(Long siteId) {
        if (siteId == null) return List.of();
        try {
            return coreDailyInfoClient.getDailyInfos(siteId);
        } catch (Exception e) {
            log.warn("[SttWS] dailyInfos 조회 실패 (빈 context 사용): siteId={}, error={}", siteId, e.getMessage());
            return List.of();
        }
    }

    /** FastAPI final_text 수신 후 Core에 대화 이력 저장. 실패해도 WS 흐름을 끊지 않음 */
    private void saveWsChatHistory(String sessionId, DeviceDetails device, Long siteId,
                                   String language, String query, String answer, String category) {
        try {
            String deviceId = device != null ? device.getDeviceId() : "unknown";
            String normalizedLanguage = normalizeLanguage(language);
            coreChatClient.saveWsMessage(sessionId, WsChatSaveCommand.builder()
                    .sessionId(sessionId)
                    .deviceId(deviceId)
                    .siteId(siteId)
                    .question(query)
                    .answer(answer)
                    .language(normalizedLanguage)
                    .category(category)
                    .build());
        } catch (Exception e) {
            log.warn("[SttWS] 채팅 이력 저장 실패 (무시): sessionId={}, error={}", sessionId, e.getMessage());
        }
    }

    /** FastAPI WS 연결 직후 전송할 start 메시지 JSON 생성. 직렬화 실패 시 mascot 없이 최소 JSON 반환 */
    private String buildStartPayload(String sessionId, Long siteId, String deviceId, String languageCode, int sampleRate,
                                     boolean ttsStream, KioskMascotDto mascot, List<DailyInfoDto> dailyInfos,
                                     Map<String, Object> deviceLocation) {
        try {
            Map<String, Object> start = new HashMap<>();
            start.put("type", "start");
            start.put("sessionId", sessionId);
            start.put("siteId", siteId);
            start.put("deviceId", deviceId);
            start.put("language", languageCode);
            start.put("systemPrompt", mascot != null ? mascot.getSystemPrompt() : null);
            start.put("name", mascot != null ? mascot.getName() : null);
            start.put("greetingMsg", mascot != null ? mascot.getGreetingMsg() : null);
            start.put("promptConfig", mascot != null && mascot.getPromptConfig() != null ? mascot.getPromptConfig() : Map.of());
            if (deviceLocation != null) {
                start.put("deviceLocation", deviceLocation);
            }
            start.put("sampleRateHz", sampleRate);
            start.put("interimResults", true);
            start.put("ttsStream", ttsStream);
            start.put("realtime", true);
            start.put("context", buildContextPayload(dailyInfos));
            return objectMapper.writeValueAsString(start);
        } catch (Exception e) {
            log.warn("[SttWS] startPayload 직렬화 실패, mascot 없이 전송: {}", e.getMessage());
            return String.format(
                    "{\"type\":\"start\",\"sessionId\":\"%s\",\"siteId\":%d,\"deviceId\":\"%s\",\"language\":\"%s\","
                            + "\"sampleRateHz\":%d,\"interimResults\":true,"
                            + "\"ttsStream\":%b,\"realtime\":true}",
                    escapeJson(sessionId), siteId, escapeJson(deviceId), escapeJson(languageCode), sampleRate, ttsStream
            );
        }
    }

    /** WebSocket start 메시지에 실을 QaRequest context payload 생성 */
    private Map<String, Object> buildContextPayload(List<DailyInfoDto> dailyInfos) {
        Map<String, Object> context = new HashMap<>();
        context.put("dailyInfos", buildDailyInfosPayload(dailyInfos));
        return context;
    }

    private List<Map<String, Object>> buildDailyInfosPayload(List<DailyInfoDto> dailyInfos) {
        if (dailyInfos == null || dailyInfos.isEmpty()) return List.of();
        return dailyInfos.stream()
                .map(di -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("placeName", di.getPlaceName());
                    item.put("infoType", di.getInfoType());
                    item.put("content", di.getContent());
                    return item;
                })
                .toList();
    }

    private Map<String, Object> buildDeviceLocationPayload(DeviceDetails device) {
        if (device == null || device.getLatitude() == null || device.getLongitude() == null) {
            return null;
        }
        Map<String, Object> location = new HashMap<>();
        location.put("latitude", device.getLatitude());
        location.put("longitude", device.getLongitude());
        return location;
    }

    // Fallback JSON escaping for manual start payload assembly.
    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "ko";
        }
        String normalized = language.split("-", 2)[0].trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? "ko" : normalized;
    }

    /** WS URI 쿼리 파라미터(?key=value&...)를 Map으로 파싱. URL 인코딩된 값도 디코딩 */
    private Map<String, String> parseQueryParams(WebSocketSession session) {
        Map<String, String> params = new HashMap<>();
        java.net.URI uri = session.getUri();
        String query = uri != null ? uri.getRawQuery() : null;
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

    /** 핸드셰이크 인터셉터가 JWT 인증 후 세션 attributes에 저장해 둔 DeviceDetails 조회 */
    private DeviceDetails getDeviceDetails(WebSocketSession session) {
        return (DeviceDetails) session.getAttributes()
                .get(DeviceTokenHandshakeInterceptor.DEVICE_DETAILS_ATTR);
    }

    /** 문자열을 양의 정수로 파싱. 파싱 실패 또는 0 이하면 defaultValue 반환 */
    private int parsePositiveIntOrDefault(String value, int defaultValue) {
        if (value == null) return defaultValue;
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
