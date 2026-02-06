package com.guideon.guideonbackend.domain.zone.service;

import com.guideon.guideonbackend.domain.admin.entity.AdminRole;
import com.guideon.guideonbackend.domain.admin.repository.AdminSiteRepository;
import com.guideon.guideonbackend.domain.site.entity.Site;
import com.guideon.guideonbackend.domain.site.repository.SiteRepository;
import com.guideon.guideonbackend.domain.zone.dto.CreateZoneRequest;
import com.guideon.guideonbackend.domain.zone.dto.DeleteZoneResponse;
import com.guideon.guideonbackend.domain.zone.dto.ZoneResponse;
import com.guideon.guideonbackend.domain.zone.entity.Zone;
import com.guideon.guideonbackend.domain.zone.entity.ZoneType;
import com.guideon.guideonbackend.domain.zone.repository.ZoneRepository;
import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import com.guideon.guideonbackend.global.security.CustomAdminDetails;
import com.guideon.guideonbackend.global.util.GeoJsonUtil;
import com.guideon.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
     * 구역 목록 조회 (zone_type, parent_zone_id 필터, 페이지네이션)
     * zone_type과 parent_zone_id로 추가 필터 검색 가능
     * 두 정보 비워둘 시 site에 해당하는 모든 zone 출력
     */
    public PageResponse<ZoneResponse> getZones(Long siteId, String zoneTypeStr, Long parentZoneId,
                                                Pageable pageable, CustomAdminDetails adminDetails) {
        validateSiteAccess(adminDetails, siteId);

        if (!siteRepository.existsById(siteId)) {
            throw new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 관광지입니다: " + siteId);
        }

        Page<Zone> zonePage;
        if (zoneTypeStr != null && parentZoneId != null) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "zone_type과 parent_zone_id는 동시에 사용할 수 없습니다");
        } else if (zoneTypeStr != null) {
            ZoneType zoneType;
            try {
                zoneType = ZoneType.valueOf(zoneTypeStr);
            } catch (IllegalArgumentException e) {
                throw new CustomException(ErrorCode.VALIDATION_ERROR, "유효하지 않은 구역 타입입니다: " + zoneTypeStr);
            }
            zonePage = zoneRepository.findBySite_SiteIdAndZoneType(siteId, zoneType, pageable);
        } else if (parentZoneId != null) {
            zoneRepository.findByZoneIdAndSite_SiteId(parentZoneId, siteId)
                    .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND,
                            "존재하지 않는 부모 구역입니다: " + parentZoneId));
            zonePage = zoneRepository.findBySite_SiteIdAndParentZone_ZoneId(siteId, parentZoneId, pageable);
        } else {
            zonePage = zoneRepository.findBySite_SiteId(siteId, pageable);
        }

        return PageResponse.from(zonePage.map(ZoneResponse::from));
    }

    /**
     * 구역 상세 조회
     */
    public ZoneResponse getZone(Long siteId, Long zoneId, CustomAdminDetails adminDetails) {
        validateSiteAccess(adminDetails, siteId);

        Zone zone = zoneRepository.findByZoneIdAndSite_SiteId(zoneId, siteId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 구역입니다: " + zoneId));

        return ZoneResponse.from(zone);
    }

    /**
     * 구역 삭제
     * - INNER 삭제 시 자식 SUB도 함께 삭제
     * - SUB는 단독 삭제 가능
     */
    @Transactional
    public DeleteZoneResponse deleteZone(Long siteId, Long zoneId, CustomAdminDetails adminDetails) {
        validateSiteAccess(adminDetails, siteId);

        // Zone 존재 여부 확인
        Zone zone = zoneRepository.findByZoneIdAndSite_SiteId(zoneId, siteId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 구역입니다: " + zoneId));

        // INNER 삭제 시 자식 SUB 함께 삭제
        if (zone.getZoneType() == ZoneType.INNER) {
            List<Zone> childZones = zoneRepository.findByParentZone_ZoneId(zoneId);
            if (!childZones.isEmpty()) {
                zoneRepository.deleteAll(childZones);
            }
        }

        // Zone 삭제
        zoneRepository.delete(zone);

        return DeleteZoneResponse.of(zoneId);
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
