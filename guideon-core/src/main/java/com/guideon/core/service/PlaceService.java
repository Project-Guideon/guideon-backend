package com.guideon.core.service;

import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import com.guideon.core.domain.place.entity.Place;
import com.guideon.core.domain.place.entity.ZoneSource;
import com.guideon.core.domain.place.repository.PlaceRepository;
import com.guideon.core.domain.site.entity.Site;
import com.guideon.core.domain.site.repository.SiteRepository;
import com.guideon.core.domain.zone.entity.Zone;
import com.guideon.core.domain.zone.repository.ZoneRepository;
import com.guideon.core.dto.CreatePlaceCommand;
import com.guideon.core.dto.PlaceDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service("corePlaceService")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final SiteRepository siteRepository;
    private final ZoneRepository zoneRepository;

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    /**
     * 장소 생성
     * zone_source가 AUTO일 때 좌표 기반으로 zone 자동 할당
     */
    @Transactional
    public PlaceDto createPlace(Long siteId, CreatePlaceCommand command) {
        Site site = findSiteById(siteId);

        // Site 활성화 상태 확인
        if (!site.getIsActive()) {
            throw new CustomException(ErrorCode.SITE_INACTIVE);
        }

        // 좌표 검증
        validateCoordinates(command.getLatitude(), command.getLongitude());

        // ZoneSource 파싱
        ZoneSource zoneSource = parseZoneSource(command.getZoneSource());

        // Zone 자동 할당 (AUTO일 때)
        Zone zone = null;
        if (zoneSource == ZoneSource.AUTO) {
            Long zoneId = placeRepository.findZoneIdByCoordinates(
                    siteId,
                    command.getLatitude(),
                    command.getLongitude()
            );
            if (zoneId != null) {
                zone = zoneRepository.findById(zoneId).orElse(null);
            }
            // zone이 null이면 OUTER (어떤 Zone에도 포함되지 않음)
        }

        // 좌표 -> Point 변환 (PostGIS geography 타입)
        Point location = createPoint(command.getLongitude(), command.getLatitude());

        Place place = Place.builder()
                .site(site)
                .zone(zone)
                .zoneSource(zoneSource)
                .name(command.getName())
                .nameJson(command.getNameJson())
                .category(command.getCategory())
                .location(location)
                .description(command.getDescription())
                .imageUrl(command.getImageUrl())
                .isActive(command.getIsActive())
                .build();

        Place saved = placeRepository.save(place);
        log.info("장소 생성 완료: placeId={}, siteId={}, zoneId={}, name={}",
                saved.getPlaceId(), siteId, zone != null ? zone.getZoneId() : null, command.getName());

        return PlaceDto.from(saved);
    }

    private Site findSiteById(Long siteId) {
        return siteRepository.findById(siteId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 관광지입니다: " + siteId));
    }

    private ZoneSource parseZoneSource(String zoneSourceStr) {
        if (zoneSourceStr == null) {
            return ZoneSource.AUTO;
        }
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
        if (latitude < -90 || latitude > 90) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "위도는 -90 ~ 90 범위여야 합니다");
        }
        if (longitude < -180 || longitude > 180) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "경도는 -180 ~ 180 범위여야 합니다");
        }
    }

    private Point createPoint(Double longitude, Double latitude) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
    }
}
