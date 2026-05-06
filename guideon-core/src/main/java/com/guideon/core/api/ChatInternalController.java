package com.guideon.core.api;

import com.guideon.core.dto.chat.ChatCommand;
import com.guideon.core.dto.chat.ChatResult;
import com.guideon.core.dto.chat.WsChatSaveCommand;
import com.guideon.core.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 채팅 내부 API
 *
 * Kiosk BFF → Core: 세션 생성 + 메시지 처리
 */
@RestController
@RequestMapping("/internal/v1/chat")
@RequiredArgsConstructor
public class ChatInternalController {

    private final ChatService chatService;

    @PostMapping("/sessions")
    public ResponseEntity<Map<String, String>> createSession(
            @RequestParam String deviceId,
            @RequestParam Long siteId
    ) {
        String sessionId = chatService.createSession(deviceId, siteId);
        return ResponseEntity.ok(Map.of("sessionId", sessionId));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<ChatResult> sendMessage(
            @PathVariable String sessionId,
            @RequestBody ChatCommand command
    ) {
        ChatResult result = chatService.sendMessage(command);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /internal/v1/chat/sessions/{sessionId}/end
     * 세션 종료 — DB ended_at 기록 + Redis 대화 내역 삭제
     * 키오스크 종료 버튼 클릭 시 Kiosk BFF가 호출
     */
    @PostMapping("/sessions/{sessionId}/end")
    public ResponseEntity<Void> endSession(
            @PathVariable String sessionId,
            @RequestParam String deviceId
    ) {
        chatService.endSession(sessionId, deviceId);
        return ResponseEntity.ok().build();
    }

    /**
     * POST /internal/v1/chat/sessions/{sessionId}/ws-message
     * WebSocket STT 파이프라인 완료 후 채팅 이력 저장 (Kiosk BFF 호출)
     */
    @PostMapping("/sessions/{sessionId}/ws-message")
    public ResponseEntity<Void> saveWsMessage(
            @PathVariable String sessionId,
            @RequestBody WsChatSaveCommand command
    ) {
        chatService.saveWsMessage(sessionId, command);
        return ResponseEntity.ok().build();
    }
}
