package com.guideon.core.domain.chat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 대화 메시지 로그
 *
 * 질문/답변 쌍을 한 레코드로 저장.
 * 통계 대시보드용: 질문 유형, AI 응답 성공률, 응답 시간.
 */
@Entity
@Table(name = "tb_chat_message", indexes = {
        @Index(name = "idx_chat_msg_session", columnList = "session_id"),
        @Index(name = "idx_chat_msg_site_created", columnList = "site_id, created_at"),
        @Index(name = "idx_chat_msg_category", columnList = "site_id, category")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Column(name = "device_id", nullable = false, length = 50)
    private String deviceId;

    // --- 질문 ---
    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "language", length = 10)
    private String language;

    // --- 답변 ---
    @Column(name = "answer", columnDefinition = "TEXT")
    private String answer;

    @Column(name = "emotion", length = 20)
    private String emotion;

    @Column(name = "place_id")
    private Long placeId;

    // --- 통계/분석용 ---
    @Column(name = "category", length = 30)
    private String category;          // DIRECTION, HOURS, FACILITY, HISTORY, GENERAL 등

    @Column(name = "answer_found", nullable = false)
    private boolean answerFound;      // AI가 의미 있는 답변을 찾았는지

    @Column(name = "response_time_ms")
    private Long responseTimeMs;      // FastAPI 응답 시간 (밀리초)

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
