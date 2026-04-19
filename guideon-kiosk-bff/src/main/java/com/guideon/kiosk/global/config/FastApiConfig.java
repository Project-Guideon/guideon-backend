package com.guideon.kiosk.global.config;

import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * FastAPI 연동 설정
 *
 * - OkHttpClient Bean: WebSocket 연결용 (read timeout 0 = 무제한, WS keepalive에 필요)
 * - getWsStreamUrl(): http(s) → ws(s) 변환 후 /ws/stream 경로 반환
 */
@Configuration
public class FastApiConfig {

    @Value("${fastapi.service.url:http://localhost:8000}")
    private String fastapiServiceUrl;

    @Value("${fastapi.websocket.connect-timeout:5}") private int connectTimeout;
    @Value("${fastapi.websocket.read-timeout:0}") private int readTimeout;
    @Value("${fastapi.websocket.write-timeout:10}") private int writeTimeout;
    @Value("${fastapi.websocket.ping-interval:30}") private int pingInterval;

    @Bean(name = "fastapiOkHttpClient")
    public OkHttpClient fastapiOkHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)     // 0 = 무제한 (WebSocket keepalive)
                .writeTimeout(writeTimeout, TimeUnit.SECONDS)
                .pingInterval(pingInterval, TimeUnit.SECONDS)   // 서버 keepalive
                .build();
    }

    /**
     * FastAPI WebSocket 스트림 URL
     * application.yml: fastapi.service.url=http://localhost:8000
     * → ws://localhost:8000/ws/stream
     */
    public String getWsStreamUrl() {
        return fastapiServiceUrl
                .replaceFirst("^https://", "wss://")
                .replaceFirst("^http://", "ws://")
                + "/ws/stream";
    }
}
