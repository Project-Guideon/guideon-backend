package com.guideon.guideonbackend.domain.zone.controller;

import com.guideon.guideonbackend.domain.zone.dto.CreateZoneRequest;
import com.guideon.guideonbackend.domain.zone.dto.ZoneResponse;
import com.guideon.guideonbackend.domain.zone.service.ZoneService;
import com.guideon.guideonbackend.global.response.ApiResponse;
import com.guideon.guideonbackend.global.trace.TraceIdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "구역 관리", description = "구역(Zone) CRUD API")
@RestController
@RequestMapping("/api/v1/admin/sites/{siteId}/zones")
@RequiredArgsConstructor
public class ZoneController {

    private final ZoneService zoneService;

    @Operation(summary = "구역 생성", description = "관광지 내 새로운 구역을 생성합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<ZoneResponse>> createZone(
            @PathVariable Long siteId,
            @Valid @RequestBody CreateZoneRequest request,
            HttpServletRequest httpRequest
    ) {
        ZoneResponse response = zoneService.createZone(siteId, request);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }
}
