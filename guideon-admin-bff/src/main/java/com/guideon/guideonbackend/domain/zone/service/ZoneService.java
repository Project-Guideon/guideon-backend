package com.guideon.guideonbackend.domain.zone.service;

import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import com.guideon.common.response.PageResponse;
import com.guideon.core.domain.admin.entity.AdminRole;
import com.guideon.core.domain.admin.repository.AdminSiteRepository;
import com.guideon.core.dto.CreateZoneCommand;
import com.guideon.core.dto.DeleteZoneResult;
import com.guideon.core.dto.ZoneDto;
import com.guideon.guideonbackend.client.CoreZoneClient;
import com.guideon.guideonbackend.domain.zone.dto.CreateZoneRequest;
import com.guideon.guideonbackend.domain.zone.dto.DeleteZoneResponse;
import com.guideon.guideonbackend.domain.zone.dto.ZoneResponse;
import com.guideon.guideonbackend.global.security.CustomAdminDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * Admin BFF Zone Service
 * Core Service를 Feign Client로 호출하여 Zone 관련 기능 제공
 * 인증/인가(Site Scope 검증)는 BFF에서 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZoneService {

    private final CoreZoneClient coreZoneClient;
    private final AdminSiteRepository adminSiteRepository;

    /**
     * 구역 생성
     */
    public ZoneResponse createZone(Long siteId, CreateZoneRequest request, CustomAdminDetails adminDetails) {
        validateSiteAccess(adminDetails, siteId);

        CreateZoneCommand command = CreateZoneCommand.builder()
                .name(request.getName())
                .code(request.getCode())
                .zoneType(request.getZoneType())
                .parentZoneId(request.getParentZoneId())
                .areaGeojson(request.getAreaGeojson())
                .build();

        ZoneDto zoneDto = coreZoneClient.createZone(siteId, command);
        log.info("구역 생성 완료: zoneId={}, siteId={}, code={}", zoneDto.getZoneId(), siteId, zoneDto.getCode());

        return ZoneResponse.from(zoneDto);
    }

    /**
     * 구역 목록 조회 (zone_type, parent_zone_id 필터, 페이지네이션)
     */
    public PageResponse<ZoneResponse> getZones(Long siteId, String zoneType, Long parentZoneId,
                                                Pageable pageable, CustomAdminDetails adminDetails) {
        validateSiteAccess(adminDetails, siteId);

        String sortParam = convertSortToString(pageable.getSort());

        Page<ZoneDto> zonePage = coreZoneClient.getZones(
                siteId,
                zoneType,
                parentZoneId,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sortParam
        );

        return PageResponse.from(zonePage.map(ZoneResponse::from));
    }

    /**
     * Spring Sort 객체를 쿼리 파라미터 문자열로 변환
     * 예: "zoneId,desc" 또는 "name,asc"
     */
    private String convertSortToString(Sort sort) {
        if (sort.isUnsorted()) {
            return "zoneId,asc"; // 기본 정렬
        }
        return sort.stream()
                .map(order -> order.getProperty() + "," + order.getDirection().name().toLowerCase())
                .collect(Collectors.joining(","));
    }

    /**
     * 구역 상세 조회
     */
    public ZoneResponse getZone(Long siteId, Long zoneId, CustomAdminDetails adminDetails) {
        validateSiteAccess(adminDetails, siteId);

        ZoneDto zoneDto = coreZoneClient.getZone(siteId, zoneId);
        return ZoneResponse.from(zoneDto);
    }

    /**
     * 구역 삭제
     */
    public DeleteZoneResponse deleteZone(Long siteId, Long zoneId, CustomAdminDetails adminDetails) {
        validateSiteAccess(adminDetails, siteId);

        DeleteZoneResult result = coreZoneClient.deleteZone(siteId, zoneId);
        log.info("구역 삭제 완료: zoneId={}, siteId={}", zoneId, siteId);

        return DeleteZoneResponse.from(result);
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
}
