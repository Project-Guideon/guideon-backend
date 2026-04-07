package com.guideon.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

/**
 * 대화 내역 Redis 관리 서비스
 *
 * 키: chat:{sessionId}
 * 값: JSON 직렬화된 메시지 리스트 (role/content)
 * TTL: 30분 (세션 활동 시마다 갱신, 키오스크 종료 시 즉시 삭제)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private static final String KEY_PREFIX = "chat:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 대화 1턴(질문 + 답변) 저장
     * - JSON 직렬화 실패 시 Redis 저장 생략 (chat 흐름은 유지)
     * - Redis 연결 오류 등 런타임 예외는 별도 catch로 처리
     * - TTL 적용 실패 시 경고 로그 (키가 이미 삭제된 경우 등)
     */
    public void saveTurn(String sessionId, String question, String answer) {
        String key = KEY_PREFIX + sessionId;

        // JSON 직렬화 — 실패 시 Redis 저장 생략
        final String userMsg;
        final String assistantMsg;
        try {
            userMsg = objectMapper.writeValueAsString(Map.of("role", "user", "content", question));
            assistantMsg = objectMapper.writeValueAsString(Map.of("role", "assistant", "content", answer));
        } catch (JsonProcessingException e) {
            log.warn("[ChatHistory] JSON 직렬화 실패: sessionId={}, {}", sessionId, e.getMessage());
            return;
        }

        // Redis 저장 — 연결 오류 등 런타임 예외 처리
        try {
            // rightPushAll로 두 메시지를 한 번에 저장 (네트워크 왕복 1회 절감)
            redisTemplate.opsForList().rightPushAll(key, userMsg, assistantMsg);

            Boolean ttlApplied = redisTemplate.expire(key, TTL);
            if (!Boolean.TRUE.equals(ttlApplied)) {
                log.warn("[ChatHistory] TTL 적용 실패 (키 없음?): sessionId={}", sessionId);
            }
        } catch (Exception e) {
            log.warn("[ChatHistory] Redis 저장 실패: sessionId={}, {}", sessionId, e.getMessage());
        }
    }

    /**
     * 세션 종료 시 대화 내역 삭제
     */
    public void deleteHistory(String sessionId) {
        redisTemplate.delete(KEY_PREFIX + sessionId);
    }
}
