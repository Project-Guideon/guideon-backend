package com.guideon.kiosk.domain.stt.handler;

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
 */
@Slf4j
public class FastApiStreamSession {

    private final WebSocketSession unitySession;
    private final String sessionId;
    private volatile WebSocket fastapiWs;
    private volatile boolean closed = false;

    /**
     * @param okHttpClient   FastApiConfig에서 주입받은 OkHttpClient
     * @param wsUrl          ws://host:port/ws/stream
     * @param unitySession   Unity ↔ BFF Spring WebSocket 세션
     * @param sessionId      채팅 sessionId (로깅용)
     * @param startPayload   FastAPI에 최초 전송할 JSON {"type":"start", ...}
     */
    public FastApiStreamSession(
            OkHttpClient okHttpClient,
            String wsUrl,
            WebSocketSession unitySession,
            String sessionId,
            String startPayload
    ) {
        this.unitySession = unitySession;
        this.sessionId = sessionId;

        Request request = new Request.Builder().url(wsUrl).build();
        fastapiWs = okHttpClient.newWebSocket(request, new FastApiListener(startPayload));
        log.info("[FastApiStream] FastAPI WS 연결 시도: sessionId={}, url={}", sessionId, wsUrl);
    }

    /** Unity에서 수신한 PCM 오디오 청크를 FastAPI로 중계 */
    public void relayBinary(byte[] data) {
        if (!closed && fastapiWs != null) {
            fastapiWs.send(ByteString.of(data));
        }
    }

    /** Unity에서 수신한 텍스트 제어 메시지(예: {"type":"stop"})를 FastAPI로 중계 */
    public void relayText(String text) {
        if (!closed && fastapiWs != null) {
            fastapiWs.send(text);
        }
    }

    /** Unity 연결 종료 시 FastAPI WS도 닫음 */
    public void close() {
        if (!closed) {
            closed = true;
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
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            log.error("[FastApiStream] FastAPI WS 오류: sessionId={}, error={}", sessionId, t.getMessage());
            closed = true;
            sendErrorToUnity("FASTAPI_UNAVAILABLE", "FastAPI 연결 오류: " + t.getMessage());
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            log.info("[FastApiStream] FastAPI WS 종료: sessionId={}, code={}, reason={}", sessionId, code, reason);
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
