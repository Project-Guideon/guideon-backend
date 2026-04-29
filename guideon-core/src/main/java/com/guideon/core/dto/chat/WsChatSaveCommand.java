package com.guideon.core.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * WebSocket STT 파이프라인 완료 후 Kiosk BFF → Core 채팅 이력 저장 요청
 *
 * HTTP 채팅(ChatCommand)과 달리 FastAPI QA 결과를 BFF가 받아서 전달.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WsChatSaveCommand {
    private String sessionId;
    private String deviceId;
    private Long siteId;
    private String question;
    private String answer;
    private String language;
    private String category;
    private Boolean answerFound;
    private Long responseTimeMs;
}
