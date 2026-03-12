package com.guideon.kiosk.global.security;

import com.guideon.core.domain.device.entity.Device;
import com.guideon.core.domain.device.repository.DeviceRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * X-DEVICE-TOKEN 헤더 기반 키오스크 인증 필터
 *
 * 1. X-DEVICE-TOKEN 헤더에서 평문 토큰 추출
 * 2. SHA-256 해시 후 DB 조회 (is_active = true 인 device만)
 * 3. 유효하면 DeviceDetails를 SecurityContext에 저장
 */
@Slf4j
@RequiredArgsConstructor
public class DeviceTokenAuthFilter extends OncePerRequestFilter {

    public static final String DEVICE_TOKEN_HEADER = "X-DEVICE-TOKEN";

    private final DeviceRepository deviceRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String plainToken = extractToken(request);

        if (StringUtils.hasText(plainToken)) {
            try {
                String tokenHash = sha256Hex(plainToken);
                Optional<Device> deviceOpt = deviceRepository.findByAuthTokenHashAndIsActiveTrue(tokenHash);

                if (deviceOpt.isPresent()) {
                    Device device = deviceOpt.get();
                    DeviceDetails deviceDetails = new DeviceDetails(
                            device.getDeviceId(),
                            device.getSite().getSiteId(),
                            device.getZone() != null ? device.getZone().getZoneId() : null,
                            device.getLocation() != null ? device.getLocation().getY() : null,
                            device.getLocation() != null ? device.getLocation().getX() : null
                    );

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    deviceDetails,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_DEVICE"))
                            );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Device 인증 성공: deviceId={}", device.getDeviceId());
                }
            } catch (Exception e) {
                log.error("Device 인증 중 시스템 오류: {}", e.getMessage(), e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        return request.getHeader(DEVICE_TOKEN_HEADER);
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