package com.guideon.guideonbackend.domain.zone.service;

import com.guideon.guideonbackend.domain.admin.entity.AdminRole;
import com.guideon.guideonbackend.domain.admin.repository.AdminSiteRepository;
import com.guideon.guideonbackend.domain.site.entity.Site;
import com.guideon.guideonbackend.domain.site.repository.SiteRepository;
import com.guideon.guideonbackend.domain.zone.dto.CreateZoneRequest;
import com.guideon.guideonbackend.domain.zone.dto.ZoneResponse;
import com.guideon.guideonbackend.domain.zone.entity.Zone;
import com.guideon.guideonbackend.domain.zone.entity.ZoneType;
import com.guideon.guideonbackend.domain.zone.repository.ZoneRepository;
import com.guideon.guideonbackend.global.exception.CustomException;
import com.guideon.guideonbackend.global.exception.ErrorCode;
import com.guideon.guideonbackend.global.security.CustomAdminDetails;
import com.guideon.guideonbackend.global.util.GeoJsonUtil;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ZoneService {

    private final ZoneRepository zoneRepository;
    private final SiteRepository siteRepository;
    private final AdminSiteRepository adminSiteRepository;

    @Transactional
    public ZoneResponse createZone(Long siteId, CreateZoneRequest request, CustomAdminDetails adminDetails) {
        // SITE_ADMIN 사이트 스코프 검증
        validateSiteAccess(adminDetails, siteId);

        // Site 조회
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 관광지입니다: " + siteId));

        // zone_type 파싱
        ZoneType zoneType;
        try {
            zoneType = ZoneType.valueOf(request.getZoneType());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "유효하지 않은 구역 타입입니다: " + request.getZoneType());
        }

        // 코드 중복 검증
        if (zoneRepository.existsBySite_SiteIdAndCode(siteId, request.getCode())) {
            throw new CustomException(ErrorCode.ZONE_CODE_DUPLICATE,
                    "이미 사용 중인 구역 코드입니다: " + request.getCode());
        }

        // zone_type에 따른 parent 검증
        Zone parentZone = validateAndGetParent(zoneType, siteId, request.getParentZoneId());

        // GeoJSON → JTS Geometry 변환
        Geometry areaGeometry;
        try {
            areaGeometry = GeoJsonUtil.toGeometry(request.getAreaGeojson());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
        String geoJsonStr = GeoJsonUtil.toJsonString(request.getAreaGeojson());

        // SUB 공간 검증
        if (zoneType == ZoneType.SUB) {
            validateSubZoneSpatial(parentZone.getZoneId(), geoJsonStr);
        }

        Zone zone = Zone.builder()
                .site(site)
                .name(request.getName())
                .code(request.getCode())
                .zoneType(zoneType)
                .parentZone(parentZone)
                .areaGeometry(areaGeometry)
                .build();

        Zone saved = zoneRepository.save(zone);
        return ZoneResponse.from(saved);
    }

    /**
     * SITE_ADMIN의 사이트 접근 권한 검증
     * PLATFORM_ADMIN은 모든 사이트 접근 가능
     */
    private void validateSiteAccess(CustomAdminDetails adminDetails, Long siteId) {
        if (AdminRole.SITE_ADMIN.name().equals(adminDetails.getRole())) {
            if (!adminSiteRepository.existsById_AdminIdAndId_SiteId(adminDetails.getAdminId(), siteId)) {
                throw new CustomException(ErrorCode.ADMIN_SITE_FORBIDDEN);
            }
        }
    }

    /**
     * zone_type에 따른 부모 구역 검증
     * - SUB: parent 필수 + 동일 site 소속 확인
     * - INNER: parent 금지
     */
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

    /**
     * SUB 구역 공간 검증: 부모 포함 + 형제 겹침 체크
     */
    private void validateSubZoneSpatial(Long parentId, String geoJsonStr) {
        // 부모 영역 포함 검증
        if (!zoneRepository.isContainedByParent(parentId, geoJsonStr)) {
            throw new CustomException(ErrorCode.ZONE_SUB_OUTSIDE_PARENT);
        }

        // 형제 SUB 겹침 검증
        if (zoneRepository.hasOverlappingSiblings(parentId, geoJsonStr)) {
            throw new CustomException(ErrorCode.ZONE_SUB_OVERLAP_FORBIDDEN);
        }
    }
}
