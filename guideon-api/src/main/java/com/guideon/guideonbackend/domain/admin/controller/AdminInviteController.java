package com.guideon.guideonbackend.domain.admin.controller;

import com.guideon.guideonbackend.domain.admin.dto.AcceptInviteRequest;
import com.guideon.guideonbackend.domain.admin.dto.AdminLoginResponse;
import com.guideon.guideonbackend.domain.admin.dto.CreateInviteRequest;
import com.guideon.guideonbackend.domain.admin.dto.InviteResponse;
import com.guideon.guideonbackend.domain.admin.service.AdminInviteService;
import com.guideon.guideonbackend.global.response.ApiResponse;
import com.guideon.guideonbackend.global.security.CustomAdminDetails;
import com.guideon.guideonbackend.global.trace.TraceIdUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "운영자 초대", description = "운영자 초대 생성/조회/만료/수락 API")
@RestController
@RequestMapping("/api/v1/admin/invites")
@RequiredArgsConstructor
public class AdminInviteController {

    private final AdminInviteService adminInviteService;

    // 초대 생성 (PLATFORM_ADMIN 전용)
    @PostMapping
    public ResponseEntity<ApiResponse<InviteResponse>> createInvite(
            @Valid @RequestBody CreateInviteRequest request,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        InviteResponse response = adminInviteService.createInvite(request, adminDetails.getAdminId());
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    // 초대 목록 조회 (PLATFORM_ADMIN 전용)
    @GetMapping
    public ResponseEntity<ApiResponse<List<InviteResponse>>> listInvites(
            HttpServletRequest httpRequest
    ) {
        List<InviteResponse> response = adminInviteService.listInvites();
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    // 초대 만료 처리 (PLATFORM_ADMIN 전용)
    @PostMapping("/{inviteId}/expire")
    public ResponseEntity<ApiResponse<Void>> expireInvite(
            @PathVariable Long inviteId,
            HttpServletRequest httpRequest
    ) {
        adminInviteService.expireInvite(inviteId);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(null, traceId));
    }

    // 초대 수락 (Public - 비인증)
    @PostMapping("/{inviteToken}/accept")
    public ResponseEntity<ApiResponse<AdminLoginResponse>> acceptInvite(
            @PathVariable String inviteToken,
            @Valid @RequestBody AcceptInviteRequest request,
            HttpServletRequest httpRequest
    ) {
        AdminLoginResponse response = adminInviteService.acceptInvite(inviteToken, request);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }
}
