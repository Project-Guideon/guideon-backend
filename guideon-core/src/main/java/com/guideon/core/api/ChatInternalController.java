package com.guideon.core.api;

import com.guideon.core.dto.chat.ChatCommand;
import com.guideon.core.dto.chat.ChatResult;
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
}
