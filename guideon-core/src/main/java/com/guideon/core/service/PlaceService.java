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
import com.guideon.core.dto.UpdatePlaceCommand;
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

    /**
     * 장소 목록 조회 (필터 + 페이지네이션)
     */
    public Page<PlaceDto> getPlaces(Long siteId, String keyword, String category,
                                     Long zoneId, Boolean isActive, Pageable pageable) {
        if (!siteRepository.existsById(siteId)) {
            throw new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 관광지입니다: " + siteId);
        }
        return placeRepository.findByFilters(siteId, keyword, category, zoneId, isActive, pageable)
                .map(PlaceDto::from);
    }

    /**
     * 장소 상세 조회
     */
    public PlaceDto getPlace(Long siteId, Long placeId) {
        Place place = placeRepository.findByPlaceIdAndSite_SiteId(placeId, siteId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND,
                        "존재하지 않는 장소입니다: " + placeId));
        return PlaceDto.from(place);
    }

    /**
     * 장소 수정
     * MANUAL + zoneId로 구역 고정 가능
     */
    @Transactional
    public PlaceDto updatePlace(Long siteId, Long placeId, UpdatePlaceCommand command) {
        Place place = placeRepository.findByPlaceIdAndSite_SiteId(placeId, siteId)
                .orElseThrow(() -> new CustomException(ErrorCode.PLACE_NOT_FOUND));

        // 좌표 변경 시 검증 + Point 생성
        Point newLocation = null;
        if (command.getLatitude() != null && command.getLongitude() != null) {
            validateCoordinates(command.getLatitude(), command.getLongitude());
            newLocation = createPoint(command.getLongitude(), command.getLatitude());
        }

        // 기본 필드 업데이트 (null이 아닌 필드만)
        place.update(command.getName(), command.getCategory(), command.getDescription(),
                command.getImageUrl(), command.getIsActive(), newLocation);

        // zone 변경 처리
        if (command.getZoneSource() != null) {
            ZoneSource zoneSource = parseZoneSource(command.getZoneSource());
            if (zoneSource == ZoneSource.MANUAL) {
                Zone zone = null;
                if (command.getZoneId() != null) {
                    zone = zoneRepository.findByZoneIdAndSite_SiteId(command.getZoneId(), siteId)
                            .orElseThrow(() -> new CustomException(ErrorCode.ZONE_NOT_FOUND));
                }
                place.changeZone(zone, ZoneSource.MANUAL);
            } else if (zoneSource == ZoneSource.AUTO) {
                // AUTO로 변경 시 좌표 기반 재할당
                Double lat = command.getLatitude() != null ? command.getLatitude() : place.getLocation().getY();
                Double lng = command.getLongitude() != null ? command.getLongitude() : place.getLocation().getX();
                Long autoZoneId = placeRepository.findZoneIdByCoordinates(siteId, lat, lng);
                Zone zone = autoZoneId != null ? zoneRepository.findById(autoZoneId).orElse(null) : null;
                place.changeZone(zone, ZoneSource.AUTO);
            }
        }

        log.info("장소 수정 완료: placeId={}, siteId={}", placeId, siteId);
        return PlaceDto.from(place);
    }

    /**
     * 장소 삭제
     */
    @Transactional
    public void deletePlace(Long siteId, Long placeId) {
        Place place = placeRepository.findByPlaceIdAndSite_SiteId(placeId, siteId)
                .orElseThrow(() -> new CustomException(ErrorCode.PLACE_NOT_FOUND));

        placeRepository.delete(place);
        log.info("장소 삭제 완료: placeId={}, siteId={}", placeId, siteId);
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
