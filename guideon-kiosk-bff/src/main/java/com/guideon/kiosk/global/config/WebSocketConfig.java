package com.guideon.kiosk.global.config;

import com.guideon.kiosk.domain.stt.handler.SttWebSocketHandler;
import com.guideon.kiosk.domain.tts.handler.TtsWebSocketHandler;
import com.guideon.kiosk.global.security.DeviceTokenHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 엔드포인트 등록
 *
 * raw WebSocket (STOMP 미사용) — 바이너리 오디오 전송에 적합.
 * 인증은 DeviceTokenHandshakeInterceptor에서 핸드셰이크 시 처리.
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final SttWebSocketHandler sttWebSocketHandler;
    private final TtsWebSocketHandler ttsWebSocketHandler;
    private final DeviceTokenHandshakeInterceptor handshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(sttWebSocketHandler, "/ws/v1/kiosk/stt")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("*");

        registry.addHandler(ttsWebSocketHandler, "/ws/v1/kiosk/tts")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
