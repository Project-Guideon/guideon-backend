package com.guideon.kiosk.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {

    private String sessionId;
    private String answer;
    private String emotion;
    private String language;
    private DisplayHint display;  // nullable (장소 언급 없으면 null)

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DisplayHint {
        private String type;       // "PLACE"
        private Long placeId;
        private String placeName;
        private String imageUrl;
        private Double latitude;
        private Double longitude;
    }
}
