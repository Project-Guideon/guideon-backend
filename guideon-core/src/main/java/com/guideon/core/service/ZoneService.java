package com.guideon.core.service;

import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import com.guideon.core.domain.site.entity.Site;
import com.guideon.core.domain.site.repository.SiteRepository;
import com.guideon.core.domain.zone.entity.Zone;
import com.guideon.core.domain.zone.entity.ZoneType;
import com.guideon.core.domain.zone.repository.ZoneRepository;
import com.guideon.core.dto.CreateZoneCommand;
import com.guideon.core.dto.DeleteZoneResult;
import com.guideon.core.dto.ZoneDto;
import com.guideon.core.global.util.GeoJsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Core Zone Service - 순수 비즈니스 로직
 * 인증/인가 로직 없음 (BFF에서 처리)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ZoneService {

    private final ZoneRepository zoneRepository;
    private final SiteRepository siteRepository;

    /**
     * 구역 생성
     */
    @Transactional
    public ZoneDto createZone(Long siteId, CreateZoneCommand command) {
        Site site = findSiteById(siteId);

        ZoneType zoneType = parseZoneType(command.getZoneType());

        // 코드 중복 검증
        if (zoneRepository.existsBySite_SiteIdAndCode(siteId, command.getCode())) {
            throw new CustomException(ErrorCode.ZONE_CODE_DUPLICATE,
                    "이미 사용 중인 구역 코드입니다: " + command.getCode());
        }

        // zone_type에 따른 parent 검증
        Zone parentZone = validateAndGetParent(zoneType, siteId, command.getParentZoneId());

        // GeoJSON → JTS Geometry 변환
        Geometry areaGeometry;
        try {
            areaGeometry = GeoJsonUtil.toGeometry(command.getAreaGeojson());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
        String geoJsonStr = GeoJsonUtil.toJsonString(command.getAreaGeojson());

        // SUB 공간 검증
        if (zoneType == ZoneType.SUB) {
            validateSubZoneSpatial(parentZone.getZoneId(), geoJsonStr);
        }

        Zone zone = Zone.builder()
                .site(site)
                .name(command.getName())
                .code(command.getCode())
                .zoneType(zoneType)
                .parentZone(parentZone)
                .areaGeometry(areaGeometry)
                .build();

        Zone saved = zoneRepository.save(zone);
        log.info("구역 생성 완료: zoneId={}, siteId={}, code={}", saved.getZoneId(), siteId, command.getCode());

        return ZoneDto.from(saved);
    }

    /**
     * 구역 목록 조회 (페이지네이션)
     */
    public Page<ZoneDto> getZones(Long siteId, String zoneTypeStr, Long parentZoneId, Pageable pageable) {
        validateSiteExists(siteId);

        Page<Zone> zonePage;
        if (zoneTypeStr != null && parentZoneId != null) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "zone_type과 parent_zone_id는 동시에 사용할 수 없습니다");
        } else if (zoneTypeStr != null) {
            ZoneType zoneType = parseZoneType(zoneTypeStr);
            zonePage = zoneRepository.findBySite_SiteIdAndZoneType(siteId, zoneType, pageable);
        } else if (parentZoneId != null) {
            zoneRepository.findByZoneIdAndSite_SiteId(parentZoneId, siteId)
                    .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND,
                            "존재하지 않는 부모 구역입니다: " + parentZoneId));
            zonePage = zoneRepository.findBySite_SiteIdAndParentZone_ZoneId(siteId, parentZoneId, pageable);
        } else {
            zonePage = zoneRepository.findBySite_SiteId(siteId, pageable);
        }

        return zonePage.map(ZoneDto::from);
    }

    /**
     * 구역 상세 조회
     */
    public ZoneDto getZone(Long siteId, Long zoneId) {
        Zone zone = findZoneBySiteAndId(siteId, zoneId);
        return ZoneDto.from(zone);
    }

    /**
     * 구역 삭제
     * - INNER 삭제 시 자식 SUB도 함께 삭제
     */
    @Transactional
    public DeleteZoneResult deleteZone(Long siteId, Long zoneId) {
        Zone zone = findZoneBySiteAndId(siteId, zoneId);

        // INNER 삭제 시 자식 SUB 함께 삭제
        if (zone.getZoneType() == ZoneType.INNER) {
            List<Zone> childZones = zoneRepository.findByParentZone_ZoneId(zoneId);
            if (!childZones.isEmpty()) {
                zoneRepository.deleteAll(childZones);
                log.info("자식 SUB 구역 삭제: count={}", childZones.size());
            }
        }

        zoneRepository.delete(zone);
        log.info("구역 삭제 완료: zoneId={}, siteId={}", zoneId, siteId);

        return DeleteZoneResult.of(zoneId);
    }

    private Site findSiteById(Long siteId) {
        return siteRepository.findById(siteId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 관광지입니다: " + siteId));
    }

    private void validateSiteExists(Long siteId) {
        if (!siteRepository.existsById(siteId)) {
            throw new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 관광지입니다: " + siteId);
        }
    }

    private Zone findZoneBySiteAndId(Long siteId, Long zoneId) {
        return zoneRepository.findByZoneIdAndSite_SiteId(zoneId, siteId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 구역입니다: " + zoneId));
    }

    private ZoneType parseZoneType(String zoneTypeStr) {
        try {
            return ZoneType.valueOf(zoneTypeStr);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "유효하지 않은 구역 타입입니다: " + zoneTypeStr);
        }
    }

    private Zone validateAndGetParent(ZoneType zoneType, Long siteId, Long parentZoneId) {
        if (zoneType == ZoneType.SUB) {
            if (parentZoneId == null) {
                throw new CustomException(ErrorCode.ZONE_PARENT_REQUIRED);
            }
            Zone parent = zoneRepository.findById(parentZoneId)
                    .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "부모 구역이 존재하지 않습니다: " + parentZoneId));
            if (!parent.getSite().getSiteId().equals(siteId)) {
                throw new CustomException(ErrorCode.VALIDATION_ERROR, "부모 구역은 동일 관광지 내에 있어야 합니다");
            }
            return parent;
        } else {
            if (parentZoneId != null) {
                throw new CustomException(ErrorCode.VALIDATION_ERROR, "INNER 구역은 부모를 가질 수 없습니다");
            }
            return null;
        }
    }

    private void validateSubZoneSpatial(Long parentId, String geoJsonStr) {
        if (!zoneRepository.isContainedByParent(parentId, geoJsonStr)) {
            throw new CustomException(ErrorCode.ZONE_SUB_OUTSIDE_PARENT);
        }
        if (zoneRepository.hasOverlappingSiblings(parentId, geoJsonStr)) {
            throw new CustomException(ErrorCode.ZONE_SUB_OVERLAP_FORBIDDEN);
        }
    }
}
