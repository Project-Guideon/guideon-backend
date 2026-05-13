package com.guideon.kiosk.client;

import com.guideon.core.dto.chat.ChatCommand;
import com.guideon.core.dto.chat.ChatResult;
import com.guideon.core.dto.chat.WsChatSaveCommand;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "core-chat", url = "${core.service.url}")
public interface CoreChatClient {

    @PostMapping("/internal/v1/chat/sessions")
    Map<String, String> createSession(
            @RequestParam String deviceId,
            @RequestParam Long siteId
    );

    @PostMapping("/internal/v1/chat/sessions/{sessionId}/messages")
    ChatResult sendMessage(
            @PathVariable String sessionId,
            @RequestBody ChatCommand command
    );

    /**
     * 세션 종료 — DB ended_at 기록 + Redis 대화 내역 삭제
     * 키오스크 종료 버튼 클릭 시 호출
     */
    @PostMapping("/internal/v1/chat/sessions/{sessionId}/end")
    void endSession(
            @PathVariable String sessionId,
            @RequestParam String deviceId,
            @RequestParam Long siteId
    );

    @PostMapping("/internal/v1/chat/sessions/{sessionId}/ws-message")
    void saveWsMessage(
            @PathVariable String sessionId,
            @RequestBody WsChatSaveCommand command
    );
}
