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
    private Long placeId;         // 언급된 장소 ID (nullable, display hint용)
    private String mapUrl;        // 카카오맵 도보 길찾기 URL (nullable, DIRECTION일 때만)
    private String emotion;       // 캐릭터 감정 태그 (GUIDING, HAPPY, THINKING 등)
    private String language;      // 실제 응답 언어

    // === 통계/분석용 (FastAPI가 분류) ===
    private String category;      // 질문 유형: DIRECTION, HOURS, FACILITY, HISTORY, GENERAL 등
    private boolean answerFound;  // AI가 의미 있는 답변을 찾았는지 (false = fallback 답변)
}
