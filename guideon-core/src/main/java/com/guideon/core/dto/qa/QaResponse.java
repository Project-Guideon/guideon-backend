package com.guideon.core.dto.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FastAPI /internal/v1/qa 응답 DTO
 * FastAPI가 RAG+LLM으로 생성한 답변
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QaResponse {

    private String answer;
    private Long placeId;       // 언급된 장소 ID (nullable, display hint용)
    private String emotion;     // 캐릭터 감정 태그 (GUIDING, HAPPY, THINKING 등)
    private String language;    // 실제 응답 언어
}
