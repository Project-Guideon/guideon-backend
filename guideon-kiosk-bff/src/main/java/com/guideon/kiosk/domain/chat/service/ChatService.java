package com.guideon.kiosk.domain.chat.service;

import com.guideon.core.dto.chat.ChatCommand;
import com.guideon.core.dto.chat.ChatResult;
import com.guideon.kiosk.client.CoreChatClient;
import com.guideon.kiosk.domain.chat.dto.ChatMessageRequest;
import com.guideon.kiosk.domain.chat.dto.ChatMessageResponse;
import com.guideon.kiosk.domain.chat.dto.CreateSessionResponse;
import com.guideon.kiosk.global.security.DeviceDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Kiosk BFF Chat Service
 *
 * 얇은 BFF 레이어: Core에 위임하고 응답을 Unity 포맷으로 변환만 함.
 * 비즈니스 로직(context 조립, FastAPI 호출, 이력 저장)은 Core에서 처리.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final CoreChatClient coreChatClient;

    /**
     * 대화 세션 생성 — Core에 위임
     */
    public CreateSessionResponse createSession(DeviceDetails device) {
        try {
            Map<String, String> result = coreChatClient.createSession(
                    device.getDeviceId(), device.getSiteId());
            String sessionId = result.get("sessionId");
            log.info("Chat 세션 생성: sessionId={}, deviceId={}", sessionId, device.getDeviceId());
            return new CreateSessionResponse(sessionId);
        } catch (Exception e) {
            log.error("세션 생성 실패: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 메시지 처리 — Core에 위임 + 응답 변환
     */
    public ChatMessageResponse sendMessage(
            String sessionId,
            ChatMessageRequest request,
            DeviceDetails device
    ) {
        ChatCommand command = ChatCommand.builder()
                .sessionId(sessionId)
                .deviceId(device.getDeviceId())
                .siteId(device.getSiteId())
                .message(request.getMessage())
                .language(request.getLanguage())
                .latitude(device.getLatitude())
                .longitude(device.getLongitude())
                .build();

        ChatResult result = coreChatClient.sendMessage(sessionId, command);

        // display hint 조립
        ChatMessageResponse.DisplayHint displayHint = null;
        if (result.getPlaceId() != null) {
            displayHint = ChatMessageResponse.DisplayHint.builder()
                    .type("PLACE")
                    .placeId(result.getPlaceId())
                    .placeName(result.getPlaceName())
                    .imageUrl(result.getImageUrl())
                    .latitude(result.getLatitude())
                    .longitude(result.getLongitude())
                    .build();
        }

        return ChatMessageResponse.builder()
                .sessionId(sessionId)
                .answer(result.getAnswer())
                .emotion(result.getEmotion())
                .language(result.getLanguage())
                .display(displayHint)
                .build();
    }
}
