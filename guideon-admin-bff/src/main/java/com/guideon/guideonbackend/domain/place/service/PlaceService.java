package com.guideon.guideonbackend.domain.place.service;

import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import com.guideon.core.domain.admin.entity.AdminRole;
import com.guideon.core.domain.admin.repository.AdminSiteRepository;
import com.guideon.core.dto.place.CreatePlaceCommand;
import com.guideon.core.dto.place.PlaceDto;
import com.guideon.core.dto.place.UpdatePlaceCommand;
import com.guideon.guideonbackend.client.CorePlaceClient;
import com.guideon.common.response.PageResponse;
import com.guideon.guideonbackend.domain.place.dto.CreatePlaceRequest;
import com.guideon.guideonbackend.domain.place.dto.PlaceImageUploadResponse;
import com.guideon.guideonbackend.domain.place.dto.PlaceResponse;
import com.guideon.guideonbackend.domain.place.dto.UpdatePlaceRequest;
import com.guideon.guideonbackend.global.security.CustomAdminDetails;
import com.guideon.guideonbackend.global.storage.FileStorageService;
import com.guideon.guideonbackend.global.storage.FileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


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
    private final FileStorageService fileStorageService;

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

        List<String> sortParam = convertSortToList(pageable.getSort());

        PageResponse<PlaceDto> placePage = corePlaceClient.getPlaces(
                siteId, keyword, category, zoneId, isActive,
                pageable.getPageNumber(), pageable.getPageSize(), sortParam
        );

        return PageResponse.<PlaceResponse>builder()
                .items(placePage.getItems().stream().map(PlaceResponse::from).toList())
                .page(placePage.getPage())
                .build();
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
     * 장소 삭제
     */
    public void deletePlace(Long siteId, Long placeId, CustomAdminDetails adminDetails) {
        validateSiteAccess(adminDetails, siteId);

        corePlaceClient.deletePlace(siteId, placeId);
        log.info("장소 삭제 완료: placeId={}, siteId={}", placeId, siteId);
    }

    /**
     * 장소 이미지 업로드 (선업로드 방식)
     * Place 생성 전 이미지를 미리 업로드하고 URL을 반환
     */
    public PlaceImageUploadResponse uploadPlaceImage(Long siteId, MultipartFile file,
                                                      CustomAdminDetails adminDetails) {
        validateSiteAccess(adminDetails, siteId);
        FileValidator.validateImage(file);

        String fileHash = FileValidator.computeFileHash(file);
        String storageUrl = fileStorageService.store(siteId, fileHash, file);

        log.info("장소 이미지 업로드 완료: siteId={}, storageUrl={}", siteId, storageUrl);
        return PlaceImageUploadResponse.builder()
                .imageUrl(storageUrl)
                .build();
    }

    // camelCase(API) → snake_case(native query 컬럼명) 매핑
    private static final Map<String, String> PLACE_SORT_FIELD_MAP = Map.of(
            "placeId",   "place_id",
            "name",      "name",
            "category",  "category",
            "isActive",  "is_active",
            "createdAt", "created_at",
            "updatedAt", "updated_at"
    );

    /**
     * Spring Sort 객체를 ["place_id,desc", "name,asc"] 형태의 List로 변환
     * 클라이언트가 camelCase(?sort=placeId)로 전송 → native query용 snake_case로 변환
     * Feign이 ?sort=place_id,desc&sort=name,asc 으로 직렬화함
     * 유효하지 않은 필드명은 무시
     */
    private List<String> convertSortToList(Sort sort) {
        if (sort.isUnsorted()) return null;

        List<String> result = sort.stream()
                .filter(order -> PLACE_SORT_FIELD_MAP.containsKey(order.getProperty()))
                .map(order -> PLACE_SORT_FIELD_MAP.get(order.getProperty()) + "," + order.getDirection().name().toLowerCase())
                .collect(Collectors.toList());

        return result.isEmpty() ? null : result;
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
