package com.guideon.core.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Core → Kiosk BFF 채팅 응답
 *
 * FastAPI 답변 + display hint 조립 결과.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResult {
    private String sessionId;
    private String answer;
    private String emotion;
    private String language;
    private String category;      // 질문 유형 (통계용, BFF→Unity에도 전달 가능)

    // 키오스크(디바이스) 좌표
    private Double deviceLatitude;
    private Double deviceLongitude;

    // display hint
    private Long placeId;
    private String placeName;
    private String imageUrl;
    private Double latitude;
    private Double longitude;
}
