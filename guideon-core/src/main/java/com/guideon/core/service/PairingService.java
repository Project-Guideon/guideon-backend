package com.guideon.core.service;

import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import com.guideon.core.domain.device.entity.Device;
import com.guideon.core.domain.device.repository.DeviceRepository;
import com.guideon.core.domain.pairing.entity.PairingRequest;
import com.guideon.core.domain.pairing.entity.PairingStatus;
import com.guideon.core.domain.pairing.repository.PairingRequestRepository;
import com.guideon.core.domain.place.entity.ZoneSource;
import com.guideon.core.domain.site.entity.Site;
import com.guideon.core.domain.site.repository.SiteRepository;
import com.guideon.core.domain.zone.entity.Zone;
import com.guideon.core.domain.zone.repository.ZoneRepository;
import com.guideon.core.dto.device.DeviceDto;
import com.guideon.core.dto.pairing.PairDeviceCommand;
import com.guideon.core.dto.pairing.PairingClaimResponse;
import com.guideon.core.dto.pairing.PairingCodeResponse;
import com.guideon.core.dto.pairing.PairingStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PairingService {

    private final PairingRequestRepository pairingRequestRepository;
    private final DeviceRepository deviceRepository;
    private final SiteRepository siteRepository;
    private final ZoneRepository zoneRepository;
    private final org.springframework.transaction.PlatformTransactionManager transactionManager;

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);
    private static final int CODE_LENGTH = 6;
    private static final int CODE_EXPIRY_MINUTES = 10;
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // 혼동 문자 제외 (O,0,I,1)
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 페어링 코드 생성
     * 키오스크 첫 부팅 시 호출 (인증 불필요)
     */
    public PairingCodeResponse requestPairingCode() {
        org.springframework.transaction.support.TransactionTemplate txTemplate = 
                new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        int maxAttempts = 5;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                return txTemplate.execute(status -> {
                    String code = generateCode();
                    String plainSecret = UUID.randomUUID().toString();

                    PairingRequest request = PairingRequest.builder()
                            .pairingCode(code)
                            .secretHash(sha256Hex(plainSecret))
                            .expiresAt(LocalDateTime.now().plusMinutes(CODE_EXPIRY_MINUTES))
                            .build();

                    // DIVE 예외를 잡기 위해 saveAndFlush 사용
                    request = pairingRequestRepository.saveAndFlush(request);
                    log.info("페어링 코드 발급: code={}**", code.substring(0, 4));

                    return PairingCodeResponse.of(request, plainSecret);
                });
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                if (attempt == maxAttempts - 1) {
                    log.error("페어링 코드 발급 최대 재시도 횟수 초과", e);
                    throw new CustomException(ErrorCode.INTERNAL_ERROR, "페어링 코드 생성에 실패했습니다");
                }
            }
        }
        throw new CustomException(ErrorCode.INTERNAL_ERROR, "페어링 코드 생성에 실패했습니다");
    }

    /**
     * 페어링 상태 폴링
     * 키오스크가 주기적으로 호출하여 매칭 완료 여부 확인
     */
    public PairingStatusResponse getPairingStatus(String pairingCode) {
        PairingRequest request = pairingRequestRepository.findByPairingCode(pairingCode)
                .orElseThrow(() -> new CustomException(ErrorCode.PAIRING_CODE_NOT_FOUND));

        if (request.getStatus() == PairingStatus.WAITING && request.isExpired()) {
            return PairingStatusResponse.builder()
                    .pairingCode(pairingCode)
                    .status(PairingStatus.EXPIRED.name())
                    .build();
        }

        return PairingStatusResponse.from(request);
    }

    /**
     * 페어링 결과 조회 (토큰 수령)
     * PAIRED 상태일 때 키오스크가 호출하여 토큰을 받아감
     * 토큰은 1회만 발급 — CLAIMED 상태에서 재호출 시 에러
     */
    @Transactional
    public PairingClaimResponse claimPairingResult(String pairingCode, String secret) {
        PairingRequest request = pairingRequestRepository.findByPairingCodeForUpdate(pairingCode)
                .orElseThrow(() -> new CustomException(ErrorCode.PAIRING_CODE_NOT_FOUND));

        if (request.getStatus() == PairingStatus.CLAIMED) {
            throw new CustomException(ErrorCode.PAIRING_ALREADY_CLAIMED);
        }

        if (request.getStatus() != PairingStatus.PAIRED) {
            throw new CustomException(ErrorCode.PAIRING_CODE_NOT_FOUND);
        }

        // Constant-time 대조 방식 활용
        if (secret == null) {
            throw new CustomException(ErrorCode.PAIRING_AUTH_FAILED);
        }
        String incomingHash = sha256Hex(secret);
        if (!MessageDigest.isEqual(request.getSecret().getBytes(StandardCharsets.UTF_8), incomingHash.getBytes(StandardCharsets.UTF_8))) {
            throw new CustomException(ErrorCode.PAIRING_AUTH_FAILED);
        }

        Device device = request.getDevice();
        if (device == null) {
            throw new CustomException(ErrorCode.PAIRING_CODE_NOT_FOUND);
        }

        // 새 토큰 발급 (claim 시점에 생성하여 한 번만 반환)
        String plainToken = UUID.randomUUID().toString();
        device.rotateToken(sha256Hex(plainToken));

        request.claim();

        log.info("페어링 토큰 수령 완료: deviceId={}", device.getDeviceId());
        return PairingClaimResponse.builder()
                .plainToken(plainToken)
                .device(DeviceDto.from(device))
                .build();
    }

    /**
     * 관리자가 페어링 코드로 디바이스 매칭
     * Admin BFF에서 호출
     */
    @Transactional
    public DeviceDto pairDevice(Long siteId, PairDeviceCommand command) {
        // 페어링 코드 검증 (비관적 락으로 원자적 처리)
        PairingRequest request = pairingRequestRepository.findByPairingCodeForUpdate(command.getPairingCode())
                .orElseThrow(() -> new CustomException(ErrorCode.PAIRING_CODE_NOT_FOUND));

        if (request.getStatus() == PairingStatus.PAIRED || request.getStatus() == PairingStatus.CLAIMED) {
            throw new CustomException(ErrorCode.PAIRING_CODE_ALREADY_PAIRED);
        }

        if (request.isExpired()) {
            request.expire();
            throw new CustomException(ErrorCode.PAIRING_CODE_EXPIRED);
        }

        // Site 검증
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 관광지입니다: " + siteId));
        if (!site.getIsActive()) {
            throw new CustomException(ErrorCode.SITE_INACTIVE);
        }

        // 좌표 검증
        validateCoordinates(command.getLatitude(), command.getLongitude());

        // Zone 할당
        ZoneSource zoneSource = parseZoneSource(command.getZoneSource());
        Zone zone = resolveZone(siteId, zoneSource, command.getZoneId(),
                command.getLatitude(), command.getLongitude());

        // 임시 토큰 해시로 디바이스 생성 (실제 토큰은 claim 시 발급)
        String tempToken = UUID.randomUUID().toString();
        Point location = GEOMETRY_FACTORY.createPoint(
                new Coordinate(command.getLongitude(), command.getLatitude()));

        Device device = Device.builder()
                .deviceId(command.getDeviceId())
                .authTokenHash(sha256Hex(tempToken))
                .site(site)
                .zone(zone)
                .zoneSource(zoneSource)
                .locationName(command.getLocationName())
                .location(location)
                .isActive(true)
                .build();

        try {
            device = deviceRepository.saveAndFlush(device);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.DEVICE_ID_DUPLICATE);
        }

        // 페어링 완료 처리
        request.pair(device);

        log.info("페어링 매칭 완료: deviceId={}, siteId={}", command.getDeviceId(), siteId);

        return DeviceDto.from(device);
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }

    private Zone resolveZone(Long siteId, ZoneSource zoneSource, Long zoneId, Double lat, Double lng) {
        if (zoneSource == ZoneSource.MANUAL) {
            if (zoneId == null) {
                throw new CustomException(ErrorCode.VALIDATION_ERROR, "MANUAL zone 지정 시 zoneId는 필수입니다");
            }
            return zoneRepository.findByZoneIdAndSite_SiteId(zoneId, siteId)
                    .orElseThrow(() -> new CustomException(ErrorCode.ZONE_NOT_FOUND));
        }
        Long foundZoneId = deviceRepository.findZoneIdByCoordinates(siteId, lat, lng);
        return foundZoneId != null ? zoneRepository.findById(foundZoneId).orElse(null) : null;
    }

    private ZoneSource parseZoneSource(String zoneSourceStr) {
        if (zoneSourceStr == null) return ZoneSource.AUTO;
        try {
            return ZoneSource.valueOf(zoneSourceStr);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "유효하지 않은 zone_source입니다: " + zoneSourceStr);
        }
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "위도와 경도는 필수입니다");
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "위도/경도 범위를 벗어났습니다");
        }
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
