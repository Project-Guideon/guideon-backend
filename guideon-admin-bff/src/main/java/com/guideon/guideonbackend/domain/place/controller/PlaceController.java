package com.guideon.guideonbackend.domain.place.controller;

import com.guideon.common.response.PageResponse;
import com.guideon.guideonbackend.domain.place.dto.CreatePlaceRequest;
import com.guideon.guideonbackend.domain.place.dto.PlaceResponse;
import com.guideon.guideonbackend.domain.place.dto.UpdatePlaceRequest;
import com.guideon.guideonbackend.domain.place.service.PlaceService;
import com.guideon.common.response.ApiResponse;
import com.guideon.guideonbackend.global.security.CustomAdminDetails;
import com.guideon.guideonbackend.global.trace.TraceIdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "장소 관리", description = "장소(Place) CRUD API")
@RestController
@RequestMapping("/api/v1/admin/sites/{siteId}/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    @Operation(summary = "장소 목록 조회", description = "관광지 내 장소 목록을 조회합니다. keyword, category, zone_id, is_active로 필터링 가능합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PlaceResponse>>> getPlaces(
            @PathVariable Long siteId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "zone_id", required = false) Long zoneId,
            @RequestParam(value = "is_active", required = false) Boolean isActive,
            @PageableDefault(size = 20, sort = "placeId") Pageable pageable,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        PageResponse<PlaceResponse> response = placeService.getPlaces(siteId, keyword, category, zoneId, isActive, pageable, adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    @Operation(summary = "장소 상세 조회", description = "특정 장소의 상세 정보를 조회합니다.")
    @GetMapping("/{placeId}")
    public ResponseEntity<ApiResponse<PlaceResponse>> getPlace(
            @PathVariable Long siteId,
            @PathVariable Long placeId,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        PlaceResponse response = placeService.getPlace(siteId, placeId, adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    @Operation(summary = "장소 수정", description = "장소 정보를 수정합니다. zone_source를 MANUAL로 설정하고 zone_id를 지정하면 구역을 고정할 수 있습니다.")
    @PatchMapping("/{placeId}")
    public ResponseEntity<ApiResponse<PlaceResponse>> updatePlace(
            @PathVariable Long siteId,
            @PathVariable Long placeId,
            @Valid @RequestBody UpdatePlaceRequest request,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        PlaceResponse response = placeService.updatePlace(siteId, placeId, request, adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    @Operation(summary = "장소 삭제", description = "장소를 삭제합니다.")
    @DeleteMapping("/{placeId}")
    public ResponseEntity<ApiResponse<Void>> deletePlace(
            @PathVariable Long siteId,
            @PathVariable Long placeId,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        placeService.deletePlace(siteId, placeId, adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(null, traceId));
    }

    @Operation(summary = "장소 생성", description = "관광지 내 새로운 장소를 생성합니다. zone_source가 AUTO면 좌표 기반으로 Zone이 자동 할당됩니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<PlaceResponse>> createPlace(
            @PathVariable Long siteId,
            @Valid @RequestBody CreatePlaceRequest request,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        PlaceResponse response = placeService.createPlace(siteId, request, adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }
}
