package com.guideon.kiosk.client;

import com.guideon.core.dto.chat.ChatCommand;
import com.guideon.core.dto.chat.ChatResult;
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
}
