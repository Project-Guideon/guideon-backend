package com.guideon.core.service;

import com.guideon.core.client.FastApiQaClient;
import com.guideon.core.domain.chat.entity.ChatMessage;
import com.guideon.core.domain.chat.entity.ChatSession;
import com.guideon.core.domain.chat.repository.ChatMessageRepository;
import com.guideon.core.domain.chat.repository.ChatSessionRepository;
import com.guideon.core.domain.place.entity.Place;
import com.guideon.core.domain.place.repository.PlaceRepository;
import com.guideon.core.dto.chat.ChatCommand;
import com.guideon.core.dto.chat.ChatResult;
import com.guideon.core.dto.dailyinfo.DailyInfoDto;
import com.guideon.core.dto.qa.QaRequest;
import com.guideon.core.dto.qa.QaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Core 채팅 서비스
 *
 * 세션 생성/관리 + context 조립 + FastAPI QA 호출 + 대화 이력 DB 저장.
 * 대화 이력의 Source of Truth.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final FastApiQaClient fastApiQaClient;
    private final DailyInfoService dailyInfoService;
    private final PlaceRepository placeRepository;

    /**
     * 대화 세션 생성 — UUID 생성 + DB 저장
     */
    @Transactional
    public String createSession(String deviceId, Long siteId) {
        String sessionId = UUID.randomUUID().toString();

        ChatSession session = ChatSession.builder()
                .sessionId(sessionId)
                .deviceId(deviceId)
                .siteId(siteId)
                .startedAt(LocalDateTime.now())
                .messageCount(0)
                .build();

        chatSessionRepository.save(session);
        log.info("Chat 세션 생성: sessionId={}, deviceId={}", sessionId, deviceId);
        return sessionId;
    }

    /**
     * 메시지 처리: context 조립 → FastAPI QA 호출 → 이력 저장 → display hint 조립
     */
    @Transactional
    public ChatResult sendMessage(ChatCommand command) {
        long startTime = System.currentTimeMillis();

        // 1. 세션 조회 + 메시지 카운트 증가
        ChatSession session = chatSessionRepository.findById(command.getSessionId())
                .orElseThrow(() -> {
                    log.warn("세션 없음: sessionId={}", command.getSessionId());
                    return new IllegalArgumentException("유효하지 않은 세션입니다: " + command.getSessionId());
                });
        session.incrementMessageCount();

        // 2. DailyInfo context 조립
        List<QaRequest.DailyInfoSummary> dailyInfoSummaries = buildDailyInfoContext(command.getSiteId());

        // 3. FastAPI QA 요청 조립
        QaRequest qaRequest = QaRequest.builder()
                .sessionId(command.getSessionId())
                .siteId(command.getSiteId())
                .question(command.getMessage())
                .language(command.getLanguage())
                .deviceLocation(QaRequest.DeviceLocation.builder()
                        .latitude(command.getLatitude())
                        .longitude(command.getLongitude())
                        .build())
                .context(QaRequest.QaContext.builder()
                        .dailyInfos(dailyInfoSummaries)
                        .build())
                .build();

        // 4. FastAPI 호출 (실패 시 fallback)
        QaResponse qaResponse = callFastApi(qaRequest);

        long responseTimeMs = System.currentTimeMillis() - startTime;

        // 5. 대화 이력 DB 저장
        ChatMessage chatMessage = ChatMessage.builder()
                .sessionId(command.getSessionId())
                .siteId(command.getSiteId())
                .deviceId(command.getDeviceId())
                .question(command.getMessage())
                .language(command.getLanguage())
                .answer(qaResponse.getAnswer())
                .emotion(qaResponse.getEmotion())
                .placeId(qaResponse.getPlaceId())
                .category(qaResponse.getCategory())
                .answerFound(qaResponse.isAnswerFound())
                .responseTimeMs(responseTimeMs)
                .createdAt(LocalDateTime.now())
                .build();

        chatMessageRepository.save(chatMessage);

        // 6. Display hint 조립
        return buildChatResult(command.getSessionId(), qaResponse);
    }

    private List<QaRequest.DailyInfoSummary> buildDailyInfoContext(Long siteId) {
        try {
            List<DailyInfoDto> dailyInfos = dailyInfoService.getDailyInfosBySiteAndDate(siteId, LocalDate.now());
            return dailyInfos.stream()
                    .map(di -> QaRequest.DailyInfoSummary.builder()
                            .placeName(di.getPlaceName())
                            .infoType(di.getInfoType())
                            .content(di.getContent())
                            .build())
                    .toList();
        } catch (Exception e) {
            log.warn("DailyInfo context 조회 실패 (무시하고 진행): {}", e.getMessage());
            return List.of();
        }
    }

    private QaResponse callFastApi(QaRequest request) {
        try {
            return fastApiQaClient.ask(request);
        } catch (Exception e) {
            log.warn("FastAPI QA 호출 실패 — fallback 응답 반환: {}", e.getMessage());
            return QaResponse.builder()
                    .answer("[AI 서비스 연결 중] 죄송합니다, 잠시 후 다시 시도해주세요.")
                    .placeId(null)
                    .emotion("SORRY")
                    .language(request.getLanguage() != null ? request.getLanguage() : "ko")
                    .category("ERROR")
                    .answerFound(false)
                    .build();
        }
    }

    private ChatResult buildChatResult(String sessionId, QaResponse qaResponse) {
        ChatResult.ChatResultBuilder builder = ChatResult.builder()
                .sessionId(sessionId)
                .answer(qaResponse.getAnswer())
                .emotion(qaResponse.getEmotion())
                .language(qaResponse.getLanguage())
                .category(qaResponse.getCategory());

        if (qaResponse.getPlaceId() != null) {
            try {
                Place place = placeRepository.findById(qaResponse.getPlaceId()).orElse(null);
                if (place != null) {
                    builder.placeId(place.getPlaceId())
                            .placeName(place.getName())
                            .imageUrl(place.getImageUrl())
                            .latitude(place.getLocation() != null ? place.getLocation().getY() : null)
                            .longitude(place.getLocation() != null ? place.getLocation().getX() : null);
                }
            } catch (Exception e) {
                log.warn("Place 조회 실패 (display hint 생략): placeId={}", qaResponse.getPlaceId());
            }
        }

        return builder.build();
    }
}
