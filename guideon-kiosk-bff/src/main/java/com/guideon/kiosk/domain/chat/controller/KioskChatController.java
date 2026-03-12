package com.guideon.kiosk.domain.chat.controller;

import com.guideon.common.response.ApiResponse;
import com.guideon.kiosk.domain.chat.dto.ChatMessageRequest;
import com.guideon.kiosk.domain.chat.dto.ChatMessageResponse;
import com.guideon.kiosk.domain.chat.dto.CreateSessionResponse;
import com.guideon.kiosk.domain.chat.service.ChatService;
import com.guideon.kiosk.global.security.DeviceDetails;
import com.guideon.kiosk.global.trace.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/kiosk/chat")
@RequiredArgsConstructor
public class KioskChatController {

    private final ChatService chatService;

    /**
     * POST /kiosk/chat/sessions
     * 대화 세션 생성 (Core에서 UUID 생성 + DB 저장)
     */
    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<CreateSessionResponse>> createSession(
            @AuthenticationPrincipal DeviceDetails device,
            HttpServletRequest httpRequest
    ) {
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(chatService.createSession(device), traceId));
    }

    /**
     * POST /kiosk/chat/sessions/{sessionId}/messages
     * 대화 메시지 전송 → AI 응답 + display hint 반환
     */
    @PostMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @PathVariable String sessionId,
            @Valid @RequestBody ChatMessageRequest request,
            @AuthenticationPrincipal DeviceDetails device,
            HttpServletRequest httpRequest
    ) {
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        ChatMessageResponse response = chatService.sendMessage(sessionId, request, device);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }
}
