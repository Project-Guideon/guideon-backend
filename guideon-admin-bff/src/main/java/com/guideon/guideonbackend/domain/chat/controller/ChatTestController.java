package com.guideon.guideonbackend.domain.chat.controller;

import com.guideon.common.response.ApiResponse;
import com.guideon.guideonbackend.domain.chat.dto.AdminChatStartRequest;
import com.guideon.guideonbackend.domain.chat.service.ChatTestService;
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

import java.util.Map;

@Tag(name = "채팅 테스트", description = "LangGraph 채팅 테스트 API")
@RestController
@RequestMapping("/api/v1/admin/chat")
@RequiredArgsConstructor
public class ChatTestController {

    private final ChatTestService chatTestService;

    @Operation(summary = "채팅 세션 시작", description = "테스트용 채팅 세션을 생성합니다.")
    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<Map<String, String>>> startSession(
            @RequestBody @Valid AdminChatStartRequest request,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest) {
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(chatTestService.startSession(request, adminDetails), traceId));
    }
}
