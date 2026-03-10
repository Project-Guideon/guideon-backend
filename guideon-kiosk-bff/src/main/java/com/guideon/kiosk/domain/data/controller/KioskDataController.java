package com.guideon.kiosk.domain.data.controller;

import com.guideon.common.response.ApiResponse;
import com.guideon.common.response.PageResponse;
import com.guideon.core.dto.DailyInfoDto;
import com.guideon.core.dto.PlaceDto;
import com.guideon.core.dto.SiteDto;
import com.guideon.core.dto.ZoneDto;
import com.guideon.kiosk.client.CoreDailyInfoClient;
import com.guideon.kiosk.client.CorePlaceClient;
import com.guideon.kiosk.client.CoreSiteClient;
import com.guideon.kiosk.client.CoreZoneClient;
import com.guideon.kiosk.global.security.DeviceDetails;
import com.guideon.kiosk.global.trace.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/kiosk")
@RequiredArgsConstructor
public class KioskDataController {

    private final CoreSiteClient coreSiteClient;
    private final CoreZoneClient coreZoneClient;
    private final CorePlaceClient corePlaceClient;
    private final CoreDailyInfoClient coreDailyInfoClient;

    /**
     * GET /kiosk/site
     * 현재 디바이스의 site 정보 조회
     */
    @GetMapping("/site")
    public ResponseEntity<ApiResponse<SiteDto>> getSite(
            @AuthenticationPrincipal DeviceDetails device,
            HttpServletRequest httpRequest
    ) {
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(
                coreSiteClient.getSite(device.getSiteId()), traceId));
    }

    /**
     * GET /kiosk/zones
     * 현재 site의 zone 목록 조회
     */
    @GetMapping("/zones")
    public ResponseEntity<ApiResponse<PageResponse<ZoneDto>>> getZones(
            @AuthenticationPrincipal DeviceDetails device,
            @RequestParam(required = false) String zoneType,
            HttpServletRequest httpRequest
    ) {
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(
                coreZoneClient.getZones(device.getSiteId(), zoneType, 0, 100), traceId));
    }

    /**
     * GET /kiosk/places
     * 현재 site의 장소 목록 조회 (활성 장소만)
     */
    @GetMapping("/places")
    public ResponseEntity<ApiResponse<PageResponse<PlaceDto>>> getPlaces(
            @AuthenticationPrincipal DeviceDetails device,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            HttpServletRequest httpRequest
    ) {
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(
                corePlaceClient.getPlaces(device.getSiteId(), keyword, category, zoneId, true, page, size),
                traceId));
    }

    /**
     * GET /kiosk/places/{placeId}
     * 장소 상세 조회
     */
    @GetMapping("/places/{placeId}")
    public ResponseEntity<ApiResponse<PlaceDto>> getPlace(
            @AuthenticationPrincipal DeviceDetails device,
            @PathVariable Long placeId,
            HttpServletRequest httpRequest
    ) {
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(
                corePlaceClient.getPlace(device.getSiteId(), placeId), traceId));
    }

    /**
     * GET /kiosk/daily-infos?date=YYYY-MM-DD
     * 날짜별 일일 정보 조회 (미입력 시 오늘)
     */
    @GetMapping("/daily-infos")
    public ResponseEntity<ApiResponse<List<DailyInfoDto>>> getDailyInfos(
            @AuthenticationPrincipal DeviceDetails device,
            @RequestParam(required = false) String date,
            HttpServletRequest httpRequest
    ) {
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(
                coreDailyInfoClient.getDailyInfos(device.getSiteId(), date), traceId));
    }
}