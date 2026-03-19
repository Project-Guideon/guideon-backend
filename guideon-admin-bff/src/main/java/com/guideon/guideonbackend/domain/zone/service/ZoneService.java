package com.guideon.guideonbackend.domain.zone.service;

import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import com.guideon.common.response.PageResponse;
import com.guideon.core.domain.admin.entity.AdminRole;
import com.guideon.core.domain.admin.repository.AdminSiteRepository;
import com.guideon.core.dto.zone.CreateZoneCommand;
import com.guideon.core.dto.zone.DeleteZoneResult;
import com.guideon.core.dto.zone.RecalcResultDto;
import com.guideon.core.dto.zone.UpdateZoneCommand;
import com.guideon.core.dto.zone.ZoneDto;
import com.guideon.guideonbackend.client.CoreZoneClient;
import com.guideon.guideonbackend.domain.zone.dto.CreateZoneRequest;
import com.guideon.guideonbackend.domain.zone.dto.DeleteZoneResponse;
import com.guideon.guideonbackend.domain.zone.dto.RecalcZoneResponse;
import com.guideon.guideonbackend.domain.zone.dto.UpdateZoneRequest;
import com.guideon.guideonbackend.domain.zone.dto.ZoneResponse;
import com.guideon.guideonbackend.global.security.CustomAdminDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
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

        List<String> sortParam = convertSortToList(pageable.getSort());

        PageResponse<ZoneDto> zonePage = coreZoneClient.getZones(
                siteId,
                zoneType,
                parentZoneId,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sortParam
        );

        return PageResponse.<ZoneResponse>builder()
                .items(zonePage.getItems().stream().map(ZoneResponse::from).toList())
                .page(zonePage.getPage())
                .build();
    }

    private static final Set<String> VALID_ZONE_SORT_FIELDS =
            Set.of("zoneId", "name", "code", "zoneType", "level");

    /**
     * Spring Sort 객체를 ["zoneId,desc", "name,asc"] 형태의 List로 변환
     * Feign이 ?sort=zoneId,desc&sort=name,asc 으로 직렬화함
     * 유효하지 않은 필드명(예: Swagger 플레이스홀더 "string")은 무시
     */
    private List<String> convertSortToList(Sort sort) {
        if (sort.isUnsorted()) return null;

        List<String> result = sort.stream()
                .filter(order -> VALID_ZONE_SORT_FIELDS.contains(order.getProperty()))
                .map(order -> order.getProperty() + "," + order.getDirection().name().toLowerCase())
                .collect(Collectors.toList());

        return result.isEmpty() ? null : result;
    }

    /**
     * 구역 수정
     */
    public ZoneResponse updateZone(Long siteId, Long zoneId, UpdateZoneRequest request, CustomAdminDetails adminDetails) {
        validateSiteAccess(adminDetails, siteId);

        UpdateZoneCommand command = UpdateZoneCommand.builder()
                .name(request.getName())
                .code(request.getCode())
                .areaGeojson(request.getAreaGeojson())
                .build();

        ZoneDto zoneDto = coreZoneClient.updateZone(siteId, zoneId, command);
        log.info("구역 수정 완료: zoneId={}, siteId={}", zoneId, siteId);

        return ZoneResponse.from(zoneDto);
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
     * Zone 재계산 실행
     */
    public RecalcZoneResponse recalculateZones(Long siteId, CustomAdminDetails adminDetails) {
        validateSiteAccess(adminDetails, siteId);

        RecalcResultDto result = coreZoneClient.recalculateZones(siteId);
        log.info("Zone 재계산 완료: siteId={}, places={}/{}, devices={}/{}",
                siteId, result.getUpdatedPlaces(), result.getTotalPlaces(),
                result.getUpdatedDevices(), result.getTotalDevices());

        return RecalcZoneResponse.from(result);
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
