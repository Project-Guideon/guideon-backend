package com.guideon.kiosk.domain.stt.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * FastAPI /ws/stream WebSocket 연결 관리
 *
 * Unity ↔ BFF ↔ FastAPI 릴레이:
 * - Unity → (binary PCM chunk) → BFF → FastAPI
 * - Unity → (text: {"type":"stop"}) → BFF → FastAPI
 * - FastAPI → (text: stt_interim/stt_final/tts_chunk/final_text/done/error) → BFF → Unity
 *
 * FastAPI 프로토콜:
 * 1. 연결 후 {"type":"start", "site_id":1, "language_code":"ko-KR", ...} 전송
 * 2. binary PCM 청크 스트리밍
 * 3. {"type":"stop"} 전송 → FastAPI가 STT 종료 후 QA → TTS 처리
 * 4. FastAPI가 {"type":"done"} 전송하면 파이프라인 완료
 *
 * onOpen() 이전에 도착한 프레임은 pendingQueue에 누적했다가
 * startPayload 전송 직후 flush하여 순서를 보장.
 */
@Slf4j
public class FastApiStreamSession {

    @FunctionalInterface
    public interface OnFinalText {
        void accept(String query, String answer, String category, Boolean answerFound, Long responseTimeMs);
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WebSocketSession unitySession;
    private final String sessionId;
    private volatile WebSocket fastapiWs;
    private volatile boolean closed = false;
    private volatile boolean ready = false;

    /** final_text 수신 시 호출 — (query, answer, category) */
    private final OnFinalText onFinalText;

    /** onOpen() 전에 도착한 프레임을 임시 보관 (binary: byte[], text: String) */
    private final ConcurrentLinkedQueue<Object> pendingQueue = new ConcurrentLinkedQueue<>();

    /**
     * @param okHttpClient   FastApiConfig에서 주입받은 OkHttpClient
     * @param wsUrl          ws://host:port/ws/stream
     * @param unitySession   Unity ↔ BFF Spring WebSocket 세션
     * @param sessionId      채팅 sessionId (로깅용)
     * @param startPayload   FastAPI에 최초 전송할 JSON {"type":"start", ...}
     * @param onFinalText    final_text 수신 시 콜백 (query, answer, category) — 채팅 이력 저장용
     */
    public FastApiStreamSession(
            OkHttpClient okHttpClient,
            String wsUrl,
            WebSocketSession unitySession,
            String sessionId,
            String startPayload,
            OnFinalText onFinalText
    ) {
        this.unitySession = unitySession;
        this.sessionId = sessionId;
        this.onFinalText = onFinalText != null ? onFinalText : (q, a, c, f, r) -> {};

        Request request = new Request.Builder().url(wsUrl).build();
        fastapiWs = okHttpClient.newWebSocket(request, new FastApiListener(startPayload));
        log.info("[FastApiStream] FastAPI WS 연결 시도: sessionId={}, url={}", sessionId, wsUrl);
    }

    /** Unity에서 수신한 PCM 오디오 청크를 FastAPI로 중계 */
    public void relayBinary(byte[] data) {
        if (closed) return;
        if (!ready) {
            pendingQueue.add(data);
            return;
        }
        fastapiWs.send(ByteString.of(data));
    }

    /** Unity에서 수신한 텍스트 제어 메시지(예: {"type":"stop"})를 FastAPI로 중계 */
    public void relayText(String text) {
        if (closed) return;
        if (!ready) {
            pendingQueue.add(text);
            return;
        }
        fastapiWs.send(text);
    }

    /** Unity 연결 종료 시 FastAPI WS도 닫음 */
    public void close() {
        if (!closed) {
            closed = true;
            pendingQueue.clear();
            if (fastapiWs != null) {
                fastapiWs.close(1000, "Unity disconnected");
                log.info("[FastApiStream] FastAPI WS 정상 종료: sessionId={}", sessionId);
            }
        }
    }

    /** FastAPI WebSocket 이벤트 처리 */
    private class FastApiListener extends WebSocketListener {

        private final String startPayload;

        FastApiListener(String startPayload) {
            this.startPayload = startPayload;
        }

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            log.info("[FastApiStream] FastAPI WS 연결 성공: sessionId={}", sessionId);
            webSocket.send(startPayload);

            // startPayload 전송 후 ready 플래그 설정 → 큐에 쌓인 프레임 flush
            ready = true;
            flushPendingQueue(webSocket);
        }

        /** FastAPI → BFF 텍스트 메시지: STT 결과, TTS 청크, 상태 등 → Unity로 그대로 중계 */
        @Override
        public void onMessage(WebSocket webSocket, String text) {
            if (text == null) return;
            try {
                if (unitySession.isOpen()) {
                    unitySession.sendMessage(new TextMessage(text));
                }
            } catch (IOException e) {
                log.error("[FastApiStream] Unity 전송 실패: sessionId={}, error={}", sessionId, e.getMessage());
            }
            interceptFinalText(text);
        }

        private void interceptFinalText(String text) {
            try {
                JsonNode node = MAPPER.readTree(text);
                if (!"final_text".equals(node.path("type").asText())) return;
                String query = node.path("query").asText(null);
                String answer = node.path("answer").asText(null);
                String category = normalizeCategory(node.path("category").asText(null));
                Boolean answerFound = parseAnswerFound(node);
                Long responseTimeMs = parseResponseTimeMs(node);
                if (query != null && answer != null) {
                    Thread.ofVirtual().start(() -> {
                        try {
                            onFinalText.accept(query, answer, category, answerFound, responseTimeMs);
                        } catch (Exception e) {
                            log.warn("[FastApiStream] final_text 후처리 실패: sessionId={}, error={}",
                                    sessionId, e.getMessage());
                        }
                    });
                }
            } catch (Exception e) {
                log.debug("[FastApiStream] final_text 파싱 실패 (무시): sessionId={}", sessionId);
            }
        }

        private String normalizeCategory(String category) {
            return category == null || category.isBlank() ? "GENERAL" : category;
        }

        private Boolean parseAnswerFound(JsonNode node) {
            JsonNode answerFound = node.has("answerFound") ? node.get("answerFound") : node.get("answer_found");
            return answerFound == null || answerFound.isNull() ? null : answerFound.asBoolean();
        }

        private Long parseResponseTimeMs(JsonNode node) {
            JsonNode responseTimeMs = node.has("responseTimeMs")
                    ? node.get("responseTimeMs")
                    : node.get("response_time_ms");
            return responseTimeMs == null || responseTimeMs.isNull() ? null : responseTimeMs.asLong();
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            log.error("[FastApiStream] FastAPI WS 오류: sessionId={}, error={}", sessionId, t.getMessage());
            closed = true;
            pendingQueue.clear();
            sendErrorToUnity("FASTAPI_UNAVAILABLE", "FastAPI 연결 오류: " + t.getMessage());
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            log.info("[FastApiStream] FastAPI WS 종료: sessionId={}, code={}, reason={}", sessionId, code, reason);
        }

        private void flushPendingQueue(WebSocket webSocket) {
            Object frame;
            while ((frame = pendingQueue.poll()) != null) {
                if (closed) break;
                if (frame instanceof byte[] bytes) {
                    webSocket.send(ByteString.of(bytes));
                } else if (frame instanceof String text) {
                    webSocket.send(text);
                }
            }
        }

        private void sendErrorToUnity(String code, String message) {
            try {
                if (unitySession.isOpen()) {
                    String errorJson = String.format(
                            "{\"type\":\"error\",\"code\":\"%s\",\"message\":\"%s\"}",
                            code, escapeJson(message)
                    );
                    unitySession.sendMessage(new TextMessage(errorJson));
                }
            } catch (IOException e) {
                log.debug("[FastApiStream] Unity 오류 메시지 전송 실패: sessionId={}, {}", sessionId, e.getMessage());
            }
        }

        private String escapeJson(String str) {
            if (str == null) return "";
            return str.replace("\\", "\\\\")
                      .replace("\"", "\\\"")
                      .replace("\n", "\\n")
                      .replace("\r", "\\r")
                      .replace("\t", "\\t");
        }
    }
}
