package com.guideon.core.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Kiosk BFF → Core 채팅 메시지 요청
 *
 * BFF가 디바이스 정보를 포함하여 Core에 전달.
 * Core가 context 조립 + FastAPI 호출 + 이력 저장.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatCommand {
    private String sessionId;
    private String deviceId;
    private Long siteId;
    private String message;
    private String language;
    private Double latitude;
    private Double longitude;
}
