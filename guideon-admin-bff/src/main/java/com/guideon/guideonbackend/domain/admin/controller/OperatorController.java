package com.guideon.guideonbackend.domain.admin.controller;

import com.guideon.common.response.ApiResponse;
import com.guideon.guideonbackend.domain.admin.dto.OperatorResponse;
import com.guideon.guideonbackend.domain.admin.dto.UpdateOperatorRequest;
import com.guideon.guideonbackend.domain.admin.service.OperatorService;
import com.guideon.guideonbackend.global.trace.TraceIdUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "운영자 관리", description = "사이트 운영자 목록 조회 및 상태 변경 API")
@RestController
@RequestMapping("/api/v1/admin/sites/{siteId}/operators")
@RequiredArgsConstructor
public class OperatorController {

    private final OperatorService operatorService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<OperatorResponse>>> getSiteOperators(
            @PathVariable Long siteId,
            HttpServletRequest httpRequest
    ) {
        List<OperatorResponse> response = operatorService.getSiteOperators(siteId);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    @PatchMapping("/{operatorId}")
    public ResponseEntity<ApiResponse<OperatorResponse>> updateOperatorStatus(
            @PathVariable Long siteId,
            @PathVariable Long operatorId,
            @RequestBody UpdateOperatorRequest request,
            HttpServletRequest httpRequest
    ) {
        OperatorResponse response = operatorService.updateOperatorStatus(siteId, operatorId, request);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }
}
