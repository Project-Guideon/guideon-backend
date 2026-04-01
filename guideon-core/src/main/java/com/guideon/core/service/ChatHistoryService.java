package com.guideon.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

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
     */
    public void saveTurn(String sessionId, String question, String answer) {
        String key = KEY_PREFIX + sessionId;
        try {
            String userMsg = objectMapper.writeValueAsString(Map.of("role", "user", "content", question));
            String assistantMsg = objectMapper.writeValueAsString(Map.of("role", "assistant", "content", answer));

            redisTemplate.opsForList().rightPush(key, Objects.requireNonNull(userMsg));
            redisTemplate.opsForList().rightPush(key, Objects.requireNonNull(assistantMsg));
            redisTemplate.expire(key, Objects.requireNonNull(TTL));
        } catch (JsonProcessingException e) {
            log.warn("[ChatHistory] 저장 실패: sessionId={}, {}", sessionId, e.getMessage());
        }
    }

    /**
     * 세션 종료 시 대화 내역 삭제
     */
    public void deleteHistory(String sessionId) {
        redisTemplate.delete(KEY_PREFIX + sessionId);
    }
}
