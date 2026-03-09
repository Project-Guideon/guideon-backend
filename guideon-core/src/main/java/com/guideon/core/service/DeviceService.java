package com.guideon.core.service;

import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import com.guideon.core.domain.device.entity.Device;
import com.guideon.core.domain.device.repository.DeviceRepository;
import com.guideon.core.domain.place.entity.ZoneSource;
import com.guideon.core.domain.site.entity.Site;
import com.guideon.core.domain.site.repository.SiteRepository;
import com.guideon.core.domain.zone.entity.Zone;
import com.guideon.core.domain.zone.repository.ZoneRepository;
import com.guideon.core.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final SiteRepository siteRepository;
    private final ZoneRepository zoneRepository;

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    /**
     * 디바이스 등록
     * auth_token은 UUID로 생성, SHA-256 해시만 DB 저장
     * 평문 토큰은 응답에 한 번만 포함 (이후 재조회 불가)
     */
    @Transactional
    public RotateTokenResult createDevice(Long siteId, CreateDeviceCommand command) {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 관광지입니다: " + siteId));

        if (!site.getIsActive()) {
            throw new CustomException(ErrorCode.SITE_INACTIVE);
        }

        if (deviceRepository.existsByDeviceId(command.getDeviceId())) {
            throw new CustomException(ErrorCode.DEVICE_ID_DUPLICATE);
        }

        validateCoordinates(command.getLatitude(), command.getLongitude());

        ZoneSource zoneSource = parseZoneSource(command.getZoneSource());
        Zone zone = resolveZone(siteId, zoneSource, command.getZoneId(),
                command.getLatitude(), command.getLongitude());

        String plainToken = UUID.randomUUID().toString();
        Point location = createPoint(command.getLongitude(), command.getLatitude());

        Device device = Device.builder()
                .deviceId(command.getDeviceId())
                .authTokenHash(sha256Hex(plainToken))
                .site(site)
                .zone(zone)
                .zoneSource(zoneSource)
                .locationName(command.getLocationName())
                .location(location)
                .isActive(command.getIsActive())
                .build();

        deviceRepository.save(device);
        log.info("디바이스 등록 완료: deviceId={}, siteId={}", device.getDeviceId(), siteId);

        return RotateTokenResult.builder()
                .plainToken(plainToken)
                .device(DeviceDto.from(device))
                .build();
    }

    /**
     * 디바이스 목록 조회 (페이지네이션)
     */
    public Page<DeviceDto> getDevices(Long siteId, Pageable pageable) {
        if (!siteRepository.existsById(siteId)) {
            throw new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 관광지입니다: " + siteId);
        }
        return deviceRepository.findBySite_SiteId(siteId, pageable).map(DeviceDto::from);
    }

    /**
     * 디바이스 상세 조회
     */
    public DeviceDto getDevice(Long siteId, String deviceId) {
        Device device = deviceRepository.findByDeviceIdAndSite_SiteId(deviceId, siteId)
                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND));
        return DeviceDto.from(device);
    }

    /**
     * 디바이스 수정
     */
    @Transactional
    public DeviceDto updateDevice(Long siteId, String deviceId, UpdateDeviceCommand command) {
        Device device = deviceRepository.findByDeviceIdAndSite_SiteId(deviceId, siteId)
                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND));

        Point newLocation = null;
        boolean hasLat = command.getLatitude() != null;
        boolean hasLng = command.getLongitude() != null;
        if (hasLat != hasLng) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "latitude와 longitude는 함께 제공해야 합니다");
        }
        if (hasLat) {
            validateCoordinates(command.getLatitude(), command.getLongitude());
            newLocation = createPoint(command.getLongitude(), command.getLatitude());
        }

        device.update(command.getLocationName(), newLocation, command.getIsActive());

        if (command.getZoneSource() != null) {
            ZoneSource zoneSource = parseZoneSource(command.getZoneSource());
            if (zoneSource == ZoneSource.MANUAL) {
                Zone zone = null;
                if (command.getZoneId() != null) {
                    zone = zoneRepository.findByZoneIdAndSite_SiteId(command.getZoneId(), siteId)
                            .orElseThrow(() -> new CustomException(ErrorCode.ZONE_NOT_FOUND));
                }
                device.changeZone(zone, ZoneSource.MANUAL);
            } else {
                Double lat = command.getLatitude() != null ? command.getLatitude() : device.getLocation().getY();
                Double lng = command.getLongitude() != null ? command.getLongitude() : device.getLocation().getX();
                Long autoZoneId = deviceRepository.findZoneIdByCoordinates(siteId, lat, lng);
                Zone zone = autoZoneId != null ? zoneRepository.findById(autoZoneId).orElse(null) : null;
                device.changeZone(zone, ZoneSource.AUTO);
            }
        }

        log.info("디바이스 수정 완료: deviceId={}, siteId={}", deviceId, siteId);
        return DeviceDto.from(device);
    }

    /**
     * 디바이스 삭제 (soft delete: is_active = false)
     */
    @Transactional
    public void deleteDevice(Long siteId, String deviceId) {
        Device device = deviceRepository.findByDeviceIdAndSite_SiteId(deviceId, siteId)
                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND));
        device.deactivate();
        log.info("디바이스 비활성화(soft delete): deviceId={}, siteId={}", deviceId, siteId);
    }

    /**
     * 디바이스 토큰 재발급 (rotate)
     * 새 UUID 평문 토큰을 생성하고 해시를 갱신
     * 평문은 응답에 한 번만 포함
     */
    @Transactional
    public RotateTokenResult rotateToken(Long siteId, String deviceId) {
        Device device = deviceRepository.findByDeviceIdAndSite_SiteId(deviceId, siteId)
                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND));

        String plainToken = UUID.randomUUID().toString();
        device.rotateToken(sha256Hex(plainToken));

        log.info("디바이스 토큰 재발급 완료: deviceId={}, siteId={}", deviceId, siteId);
        return RotateTokenResult.builder()
                .plainToken(plainToken)
                .device(DeviceDto.from(device))
                .build();
    }

    private Zone resolveZone(Long siteId, ZoneSource zoneSource, Long zoneId, Double lat, Double lng) {
        if (zoneSource == ZoneSource.MANUAL) {
            if (zoneId == null) return null;
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

    private Point createPoint(Double longitude, Double latitude) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
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
