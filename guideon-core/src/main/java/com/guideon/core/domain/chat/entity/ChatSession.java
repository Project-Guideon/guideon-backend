package com.guideon.core.domain.chat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 대화 세션
 *
 * 키오스크 디바이스별 대화 세션 추적.
 * sessionId는 UUID 문자열 (Kiosk BFF에서 생성).
 */
@Entity
@Table(name = "tb_chat_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatSession {

    @Id
    @Column(name = "session_id", length = 36)
    private String sessionId;

    @Column(name = "device_id", nullable = false, length = 50)
    private String deviceId;

    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "message_count", nullable = false)
    private int messageCount;

    public void incrementMessageCount() {
        this.messageCount++;
    }

    public void endSession() {
        this.endedAt = LocalDateTime.now();
    }
}
