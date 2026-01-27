package com.guideon.guideonbackend.domain.admin.controller;

import com.guideon.guideonbackend.domain.admin.dto.AdminLoginRequest;
import com.guideon.guideonbackend.domain.admin.dto.AdminLoginResponse;
import com.guideon.guideonbackend.domain.admin.dto.AdminRefreshRequest;
import com.guideon.guideonbackend.domain.admin.dto.AdminRefreshResponse;
import com.guideon.guideonbackend.domain.admin.dto.AdminSignupRequest;
import com.guideon.guideonbackend.domain.admin.service.AdminAuthService;
import com.guideon.guideonbackend.global.response.ApiResponse;
import com.guideon.guideonbackend.global.security.CustomAdminDetails;
import com.guideon.guideonbackend.global.trace.TraceIdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "관리자 인증", description = "관리자 로그인, 토큰 갱신, 로그아웃 API")
@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    //회원가입 후 자동 로그인 (Access Token, Refresh Token 발급)
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AdminLoginResponse>> signup(
            @Valid @RequestBody AdminSignupRequest request,
            HttpServletRequest httpRequest
    ) {
        AdminLoginResponse response = adminAuthService.signup(request);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    //이메일과 비밀번호로 로그인하여 Access Token과 Refresh Token을 발급
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AdminLoginResponse>> login(
            @Valid @RequestBody AdminLoginRequest request,
            HttpServletRequest httpRequest
    ) {
        AdminLoginResponse response = adminAuthService.login(request);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    //Refresh Token을 사용하여 새로운 Access Token과 Refresh Token을 발급
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AdminRefreshResponse>> refresh(
            @Valid @RequestBody AdminRefreshRequest request,
            HttpServletRequest httpRequest
    ) {
        AdminRefreshResponse response = adminAuthService.refresh(request);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }


    //Redis에 저장된 Refresh Token을 삭제하여 로그아웃. (Authorization 헤더에 Access Token 필요)
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        adminAuthService.logout(adminDetails.getAdminId());
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(null, traceId));
    }
}
