package com.guideon.guideonbackend.domain.zone.controller;

import com.guideon.guideonbackend.domain.zone.dto.CreateZoneRequest;
import com.guideon.guideonbackend.domain.zone.dto.DeleteZoneResponse;
import com.guideon.guideonbackend.domain.zone.dto.ZoneResponse;
import com.guideon.guideonbackend.domain.zone.service.ZoneService;
import com.guideon.common.response.ApiResponse;
import com.guideon.common.response.PageResponse;
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

@Tag(name = "구역 관리", description = "구역(Zone) CRUD API")
@RestController
@RequestMapping("/api/v1/admin/sites/{siteId}/zones")
@RequiredArgsConstructor
public class ZoneController {

    private final ZoneService zoneService;

    @Operation(summary = "구역 목록 조회", description = "관광지 내 구역 목록을 조회합니다. zone_type, parent_zone_id로 필터링 가능합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ZoneResponse>>> getZones(
            @PathVariable Long siteId,
            @RequestParam(value = "zone_type", required = false) String zoneType,
            @RequestParam(value = "parent_zone_id", required = false) Long parentZoneId,
            @PageableDefault(size = 20, sort = "zoneId") Pageable pageable,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        PageResponse<ZoneResponse> response = zoneService.getZones(siteId, zoneType, parentZoneId, pageable, adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    @Operation(summary = "구역 상세 조회", description = "특정 구역의 상세 정보를 조회합니다.")
    @GetMapping("/{zoneId}")
    public ResponseEntity<ApiResponse<ZoneResponse>> getZone(
            @PathVariable Long siteId,
            @PathVariable Long zoneId,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        ZoneResponse response = zoneService.getZone(siteId, zoneId, adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    @Operation(summary = "구역 생성", description = "관광지 내 새로운 구역을 생성합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<ZoneResponse>> createZone(
            @PathVariable Long siteId,
            @Valid @RequestBody CreateZoneRequest request,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        ZoneResponse response = zoneService.createZone(siteId, request, adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    @Operation(summary = "구역 삭제", description = "구역을 삭제합니다. INNER 삭제 시 자식 SUB도 함께 삭제됩니다.")
    @DeleteMapping("/{zoneId}")
    public ResponseEntity<ApiResponse<DeleteZoneResponse>> deleteZone(
            @PathVariable Long siteId,
            @PathVariable Long zoneId,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        DeleteZoneResponse response = zoneService.deleteZone(siteId, zoneId, adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }
}
