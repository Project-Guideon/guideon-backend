package com.guideon.kiosk.global.security;

import com.guideon.core.domain.device.entity.Device;
import com.guideon.core.domain.device.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

/**
 * WebSocket 핸드셰이크 시 Device Token 검증
 *
 * HTTP 핸드셰이크 요청의 X-DEVICE-TOKEN 헤더 또는
 * 쿼리 파라미터 token으로 디바이스 인증.
 * 인증 성공 시 DeviceDetails를 WS attributes에 저장.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceTokenHandshakeInterceptor implements HandshakeInterceptor {

    public static final String DEVICE_DETAILS_ATTR = "deviceDetails";
    private static final String TOKEN_HEADER = "X-DEVICE-TOKEN";
    private static final String TOKEN_PARAM = "token";

    private final DeviceRepository deviceRepository;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        String plainToken = extractToken(request);

        if (plainToken == null || plainToken.isBlank()) {
            log.warn("WS 핸드셰이크 거부: Device Token 없음");
            return false;
        }

        try {
            String tokenHash = sha256Hex(plainToken);
            Optional<Device> deviceOpt = deviceRepository.findByAuthTokenHashAndIsActiveTrue(tokenHash);

            if (deviceOpt.isEmpty()) {
                log.warn("WS 핸드셰이크 거부: 유효하지 않은 Device Token");
                return false;
            }

            Device device = deviceOpt.get();
            DeviceDetails deviceDetails = new DeviceDetails(
                    device.getDeviceId(),
                    device.getSite().getSiteId(),
                    device.getZone() != null ? device.getZone().getZoneId() : null,
                    device.getLocation() != null ? device.getLocation().getY() : null,
                    device.getLocation() != null ? device.getLocation().getX() : null
            );

            attributes.put(DEVICE_DETAILS_ATTR, deviceDetails);
            log.debug("WS 핸드셰이크 인증 성공: deviceId={}", device.getDeviceId());
            return true;

        } catch (Exception e) {
            log.error("WS 핸드셰이크 중 오류: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // no-op
    }

    private String extractToken(ServerHttpRequest request) {
        // 1) 헤더에서 추출
        String token = request.getHeaders().getFirst(TOKEN_HEADER);
        if (token != null && !token.isBlank()) {
            return token;
        }

        // 2) 쿼리 파라미터에서 추출 (WebSocket은 커스텀 헤더 제한이 있으므로)
        if (request instanceof ServletServerHttpRequest servletRequest) {
            token = servletRequest.getServletRequest().getParameter(TOKEN_PARAM);
        }
        return token;
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 찾을 수 없습니다", e);
        }
    }
}
