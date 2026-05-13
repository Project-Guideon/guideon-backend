package com.guideon.core.service;

import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import com.guideon.core.client.FastApiQaClient;
import com.guideon.core.domain.chat.entity.ChatMessage;
import com.guideon.core.domain.chat.entity.ChatSession;
import com.guideon.core.domain.chat.repository.ChatMessageRepository;
import com.guideon.core.domain.chat.repository.ChatSessionRepository;
import com.guideon.core.domain.device.entity.Device;
import com.guideon.core.domain.device.repository.DeviceRepository;
import com.guideon.core.domain.mascot.entity.Mascot;
import com.guideon.core.domain.mascot.repository.MascotRepository;
import com.guideon.core.domain.place.entity.Place;
import com.guideon.core.domain.place.repository.PlaceRepository;
import com.guideon.core.domain.zone.entity.Zone;
import com.guideon.core.domain.zone.entity.ZoneType;
import com.guideon.core.dto.chat.ChatCommand;
import com.guideon.core.dto.chat.ChatResult;
import com.guideon.core.dto.chat.WsChatSaveCommand;
import com.guideon.core.dto.dailyinfo.DailyInfoDto;
import com.guideon.core.dto.qa.QaRequest;
import com.guideon.core.dto.qa.QaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Core 채팅 서비스
 *
 * 세션 생성/관리 + context 조립(nearbyPlaces, dailyInfos) + FastAPI QA 호출 + 대화 이력 DB 저장.
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
    private final DeviceRepository deviceRepository;
    private final MascotRepository mascotRepository;
    private final ChatHistoryService chatHistoryService;

    /**
     * 세션 종료 — DB ended_at 기록 + Redis 대화 내역 삭제
     * 키오스크 종료 버튼 클릭 시 호출
     *
     * Redis 삭제는 트랜잭션 커밋 이후 afterCommit() 콜백에서 실행.
     * DB 롤백 시 Redis 삭제가 불필요하게 일어나는 것을 방지.
     */
    @Transactional
    public void endSession(String sessionId, String deviceId, Long siteId) {
        ChatSession session = chatSessionRepository.findById(sessionId).orElse(null);

        if (session == null) {
            log.warn("존재하지 않는 Chat 세션 종료 요청: sessionId={}", sessionId);
            return;
        }

        if (!session.getDeviceId().equals(deviceId)) {
            log.warn("세션 소유권 불일치: sessionId={}, 요청 deviceId=***", sessionId);
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        if (!session.getSiteId().equals(siteId)) {
            log.warn("세션 siteId 불일치: sessionId={}", sessionId);
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 이미 종료된 세션 재요청은 무시 (endedAt 덮어쓰기 방지)
        if (session.getEndedAt() != null) {
            log.info("이미 종료된 Chat 세션 재요청 무시: sessionId={}", sessionId);
            return;
        }

        session.endSession();
        log.info("Chat 세션 종료: sessionId={}", sessionId);

        // DB 커밋 완료 후 Redis 삭제 (트랜잭션 롤백 시 Redis 삭제 방지)
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                chatHistoryService.deleteHistory(sessionId);
            }
        });
    }

    /**
     * 대화 세션 생성 — UUID 생성 + DB 저장
     */
    @Transactional
    public String createSession(String deviceId, Long siteId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND));
        if (!Boolean.TRUE.equals(device.getIsActive())) {
            throw new CustomException(ErrorCode.DEVICE_INACTIVE);
        }
        if (device.getSite() == null || !device.getSite().getSiteId().equals(siteId)) {
            throw new CustomException(ErrorCode.ADMIN_SITE_FORBIDDEN);
        }

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

        // 1. 세션 조회 + 종료 여부 확인 + 메시지 카운트 증가
        ChatSession session = chatSessionRepository.findById(command.getSessionId())
                .orElseThrow(() -> {
                    log.warn("세션 없음: sessionId={}", command.getSessionId());
                    return new CustomException(ErrorCode.CHAT_SESSION_NOT_FOUND);
                });

        if (session.getEndedAt() != null) {
            log.warn("종료된 세션으로 메시지 수신: sessionId={}", command.getSessionId());
            throw new CustomException(ErrorCode.CHAT_SESSION_ENDED);
        }

        if (!session.getSiteId().equals(command.getSiteId())) {
            log.warn("세션 siteId 불일치: sessionId={}", command.getSessionId());
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        session.incrementMessageCount();

        // 2. 마스코트 systemPrompt + promptConfig 조회
        Mascot mascot = findMascot(command.getSiteId());

        // 3. DailyInfo context 조립
        List<QaRequest.DailyInfoSummary> dailyInfoSummaries = buildDailyInfoContext(command.getSiteId());

        // 4. FastAPI QA 요청 조립
        // nearbyPlaces 는 FastAPI fetch_places_node 가 category 추출 후 직접 조회
        QaRequest qaRequest = QaRequest.builder()
                .sessionId(command.getSessionId())
                .siteId(command.getSiteId())
                .deviceId(session.getDeviceId())
                .question(command.getMessage())
                .language(command.getLanguage())
                .systemPrompt(mascot != null ? mascot.getSystemPrompt() : null)
                .name(mascot != null ? mascot.getName() : null)
                .greetingMsg(mascot != null ? mascot.getGreetingMsg() : null)
                .promptConfig(mascot != null ? mascot.getPromptConfig() : null)
                .deviceLocation(buildDeviceLocation(session.getDeviceId()))
                .context(QaRequest.QaContext.builder()
                        .dailyInfos(dailyInfoSummaries)
                        .build())
                .build();

        // 6. FastAPI 호출 (실패 시 fallback)
        QaResponse qaResponse = callFastApi(qaRequest);

        long responseTimeMs = System.currentTimeMillis() - startTime;

        // 7. 대화 이력 DB 저장
        ChatMessage chatMessage = ChatMessage.builder()
                .sessionId(command.getSessionId())
                .siteId(command.getSiteId())
                .deviceId(session.getDeviceId())
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

        // 8. DB 커밋 후 Redis 저장 (커밋 실패 시 Redis에만 데이터 남는 불일치 방지)
        final String question = command.getMessage();
        final String answer = qaResponse.getAnswer();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                chatHistoryService.saveTurn(command.getSessionId(), question, answer);
            }
        });

        // 9. Display hint 조립
        return buildChatResult(command.getSessionId(), command, qaResponse);
    }

    private QaRequest.DeviceLocation buildDeviceLocation(String deviceId) {
        try {
            Device device = deviceRepository.findById(deviceId).orElse(null);
            if (device != null && device.getLocation() != null) {
                return QaRequest.DeviceLocation.builder()
                        .latitude(device.getLocation().getY())
                        .longitude(device.getLocation().getX())
                        .build();
            }
        } catch (Exception e) {
            log.warn("Device 위치 조회 실패: deviceId=***, {}", e.getMessage());
        }
        return null;
    }


    /**
     * 마스코트 조회.
     * 없으면 null 반환 (FastAPI 기본 프롬프트 사용).
     */
    private Mascot findMascot(Long siteId) {
        try {
            return mascotRepository.findBySite_SiteId(siteId).orElse(null);
        } catch (Exception e) {
            log.warn("마스코트 조회 실패 (기본 프롬프트 사용): siteId={}", siteId);
            return null;
        }
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

    private static final Map<String, String> FALLBACK_ANSWERS = Map.of(
            "ko", "[AI 서비스 연결 중] 죄송합니다, 잠시 후 다시 시도해주세요.",
            "en", "[AI service connecting] Sorry, please try again in a moment.",
            "ja", "[AI サービス接続中] 申し訳ありませんが、しばらくしてからもう一度お試しください。",
            "zh", "[AI服务连接中] 抱歉，请稍后再试。"
    );

    private QaResponse callFastApi(QaRequest request) {
        try {
            return fastApiQaClient.ask(request);
        } catch (Exception e) {
            log.warn("FastAPI QA 호출 실패 — fallback 응답 반환: {}", e.getMessage());
            String requestedLang = request.getLanguage() != null ? request.getLanguage().split("-")[0].toLowerCase() : "ko";
            String resolvedLang = FALLBACK_ANSWERS.containsKey(requestedLang) ? requestedLang : "en";
            String fallbackAnswer = FALLBACK_ANSWERS.get(resolvedLang);
            return QaResponse.builder()
                    .answer(fallbackAnswer)
                    .placeId(null)
                    .emotion("SORRY")
                    .language(resolvedLang)
                    .category("ERROR")
                    .answerFound(false)
                    .build();
        }
    }

    /**
     * WebSocket STT 파이프라인 완료 후 채팅 이력 저장.
     * FastAPI QA 결과를 Kiosk BFF가 받아서 전달하는 경우에 사용.
     */
    @Transactional
    public void saveWsMessage(String sessionId, WsChatSaveCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("WS chat save command is required");
        }

        if (command.getQuestion() == null || command.getQuestion().isBlank()
                || command.getAnswer() == null || command.getAnswer().isBlank()) {
            log.warn("WS chat save rejected: blank question/answer. sessionId={}", sessionId);
            throw new IllegalArgumentException("question/answer must not be blank");
        }

        if (command.getSessionId() != null && !Objects.equals(sessionId, command.getSessionId())) {
            log.warn("WS chat save rejected: path/body sessionId mismatch. pathSessionId={}", sessionId);
            throw new IllegalArgumentException("sessionId mismatch");
        }

        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> {
                    log.warn("WS chat save rejected: session not found. sessionId={}", sessionId);
                    return new IllegalArgumentException("invalid session: " + sessionId);
                });

        if (session.getEndedAt() != null) {
            log.warn("WS chat save rejected: session already ended. sessionId={}", sessionId);
            throw new IllegalStateException("session already ended: " + sessionId);
        }

        if (command.getSiteId() != null && !Objects.equals(session.getSiteId(), command.getSiteId())) {
            log.warn("WS chat save rejected: siteId mismatch. sessionId={}", sessionId);
            throw new SecurityException("siteId mismatch for session: " + sessionId);
        }

        if (command.getDeviceId() != null && !Objects.equals(session.getDeviceId(), command.getDeviceId())) {
            log.warn("WS chat save rejected: deviceId mismatch. sessionId={}", sessionId);
            throw new SecurityException("deviceId mismatch for session: " + sessionId);
        }

        session.incrementMessageCount();

        String category = (command.getCategory() == null || command.getCategory().isBlank())
                ? "GENERAL"
                : command.getCategory().trim();
        boolean answerFound = command.getAnswerFound() != null
                ? command.getAnswerFound()
                : !"ERROR".equalsIgnoreCase(category);

        ChatMessage chatMessage = ChatMessage.builder()
                .sessionId(sessionId)
                .siteId(session.getSiteId())
                .deviceId(session.getDeviceId())
                .question(command.getQuestion())
                .answer(command.getAnswer())
                .language(command.getLanguage())
                .answerFound(answerFound)
                .category(category)
                .responseTimeMs(command.getResponseTimeMs())
                .createdAt(LocalDateTime.now())
                .build();

        chatMessageRepository.save(chatMessage);

        final String question = command.getQuestion();
        final String answer = command.getAnswer();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                chatHistoryService.saveTurn(sessionId, question, answer);
            }
        });
    }

    private ChatResult buildChatResult(String sessionId, ChatCommand command, QaResponse qaResponse) {
        ChatResult.ChatResultBuilder builder = ChatResult.builder()
                .sessionId(sessionId)
                .answer(qaResponse.getAnswer())
                .emotion(qaResponse.getEmotion())
                .language(qaResponse.getLanguage())
                .category(qaResponse.getCategory())
                .deviceLatitude(command.getLatitude())
                .deviceLongitude(command.getLongitude());

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
