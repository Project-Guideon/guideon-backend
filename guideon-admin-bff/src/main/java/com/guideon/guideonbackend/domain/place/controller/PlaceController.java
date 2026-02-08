package com.guideon.guideonbackend.domain.place.controller;

import com.guideon.guideonbackend.domain.place.dto.CreatePlaceRequest;
import com.guideon.guideonbackend.domain.place.dto.PlaceResponse;
import com.guideon.guideonbackend.domain.place.service.PlaceService;
import com.guideon.common.response.ApiResponse;
import com.guideon.guideonbackend.global.security.CustomAdminDetails;
import com.guideon.guideonbackend.global.trace.TraceIdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "장소 관리", description = "장소(Place) CRUD API")
@RestController
@RequestMapping("/api/v1/admin/sites/{siteId}/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

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
