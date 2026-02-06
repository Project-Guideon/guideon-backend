package com.guideon.guideonbackend.domain.site.controller;

import com.guideon.guideonbackend.domain.site.dto.CreateSiteRequest;
import com.guideon.guideonbackend.domain.site.dto.SiteResponse;
import com.guideon.guideonbackend.domain.site.dto.UpdateSiteRequest;
import com.guideon.guideonbackend.domain.site.service.SiteService;
import com.guideon.common.response.ApiResponse;
import com.guideon.guideonbackend.global.trace.TraceIdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "관광지 관리", description = "관광지(Site) CRUD API - PLATFORM_ADMIN 전용")
@RestController
@RequestMapping("/api/v1/admin/sites")
@RequiredArgsConstructor
public class SiteController {

    private final SiteService siteService;

    @Operation(summary = "관광지 생성", description = "새로운 관광지를 생성합니다. PLATFORM_ADMIN 권한 필요")
    @PostMapping
    public ResponseEntity<ApiResponse<SiteResponse>> createSite(
            @Valid @RequestBody CreateSiteRequest request,
            HttpServletRequest httpRequest
    ) {
        SiteResponse response = siteService.createSite(request);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    @Operation(summary = "관광지 목록 조회", description = "모든 관광지 목록을 조회합니다. PLATFORM_ADMIN 권한 필요")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SiteResponse>>> getAllSites(
            HttpServletRequest httpRequest
    ) {
        List<SiteResponse> response = siteService.getAllSites();
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    @Operation(summary = "관광지 상세 조회", description = "특정 관광지의 상세 정보를 조회합니다. PLATFORM_ADMIN 권한 필요")
    @GetMapping("/{siteId}")
    public ResponseEntity<ApiResponse<SiteResponse>> getSite(
            @PathVariable Long siteId,
            HttpServletRequest httpRequest
    ) {
        SiteResponse response = siteService.getSite(siteId);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    @Operation(summary = "관광지 수정", description = "관광지 정보를 수정합니다. PLATFORM_ADMIN 권한 필요")
    @PatchMapping("/{siteId}")
    public ResponseEntity<ApiResponse<SiteResponse>> updateSite(
            @PathVariable Long siteId,
            @Valid @RequestBody UpdateSiteRequest request,
            HttpServletRequest httpRequest
    ) {
        SiteResponse response = siteService.updateSite(siteId, request);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    @Operation(summary = "관광지 비활성화", description = "관광지를 비활성화합니다. PLATFORM_ADMIN 권한 필요")
    @PostMapping("/{siteId}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateSite(
            @PathVariable Long siteId,
            HttpServletRequest httpRequest
    ) {
        siteService.deactivateSite(siteId);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(null, traceId));
    }

    @Operation(summary = "관광지 재활성화", description = "비활성화된 관광지를 다시 활성화합니다. PLATFORM_ADMIN 권한 필요")
    @PostMapping("/{siteId}/activate")
    public ResponseEntity<ApiResponse<Void>> activateSite(
            @PathVariable Long siteId,
            HttpServletRequest httpRequest
    ) {
        siteService.activateSite(siteId);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(null, traceId));
    }
}
