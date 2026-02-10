package com.guideon.guideonbackend.domain.place.service;

import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import com.guideon.core.domain.admin.entity.AdminRole;
import com.guideon.core.domain.admin.repository.AdminSiteRepository;
import com.guideon.core.dto.CreatePlaceCommand;
import com.guideon.core.dto.PlaceDto;
import com.guideon.core.dto.UpdatePlaceCommand;
import com.guideon.guideonbackend.client.CorePlaceClient;
import com.guideon.common.response.PageResponse;
import com.guideon.guideonbackend.domain.place.dto.CreatePlaceRequest;
import com.guideon.guideonbackend.domain.place.dto.PlaceResponse;
import com.guideon.guideonbackend.domain.place.dto.UpdatePlaceRequest;
import com.guideon.guideonbackend.global.security.CustomAdminDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Admin BFF Place Service
 * Core Service를 Feign Client로 호출하여 Place 관련 기능 제공
 * 인증/인가(Site Scope 검증) 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceService {

    private final CorePlaceClient corePlaceClient;
    private final AdminSiteRepository adminSiteRepository;

    /**
     * 장소 생성
     */
    public PlaceResponse createPlace(Long siteId, CreatePlaceRequest request, CustomAdminDetails adminDetails) {
        validateSiteAccess(adminDetails, siteId);

        CreatePlaceCommand command = CreatePlaceCommand.builder()
                .name(request.getName())
                .nameJson(request.getNameJson())
                .category(request.getCategory())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .isActive(request.getIsActive())
                .zoneSource(request.getZoneSource())
                .build();

        PlaceDto placeDto = corePlaceClient.createPlace(siteId, command);
        log.info("장소 생성 완료: placeId={}, siteId={}, name={}", placeDto.getPlaceId(), siteId, placeDto.getName());

        return PlaceResponse.from(placeDto);
    }

    /**
     * 장소 목록 조회 (필터 + 페이지네이션)
     */
    public PageResponse<PlaceResponse> getPlaces(Long siteId, String keyword, String category,
                                                   Long zoneId, Boolean isActive,
                                                   Pageable pageable, CustomAdminDetails adminDetails) {
        validateSiteAccess(adminDetails, siteId);

        Page<PlaceDto> placePage = corePlaceClient.getPlaces(
                siteId, keyword, category, zoneId, isActive,
                pageable.getPageNumber(), pageable.getPageSize()
        );

        return PageResponse.from(placePage.map(PlaceResponse::from));
    }

    /**
     * 장소 상세 조회
     */
    public PlaceResponse getPlace(Long siteId, Long placeId, CustomAdminDetails adminDetails) {
        validateSiteAccess(adminDetails, siteId);

        PlaceDto placeDto = corePlaceClient.getPlace(siteId, placeId);
        return PlaceResponse.from(placeDto);
    }

    /**
     * 장소 수정
     */
    public PlaceResponse updatePlace(Long siteId, Long placeId, UpdatePlaceRequest request, CustomAdminDetails adminDetails) {
        validateSiteAccess(adminDetails, siteId);

        UpdatePlaceCommand command = UpdatePlaceCommand.builder()
                .name(request.getName())
                .category(request.getCategory())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .isActive(request.getIsActive())
                .zoneSource(request.getZoneSource())
                .zoneId(request.getZoneId())
                .build();

        PlaceDto placeDto = corePlaceClient.updatePlace(siteId, placeId, command);
        log.info("장소 수정 완료: placeId={}, siteId={}", placeId, siteId);

        return PlaceResponse.from(placeDto);
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
