package com.guideon.kiosk.domain.chat.service;

import com.guideon.core.dto.DailyInfoDto;
import com.guideon.core.dto.PlaceDto;
import com.guideon.core.dto.QaRequest;
import com.guideon.core.dto.QaResponse;
import com.guideon.kiosk.client.CoreDailyInfoClient;
import com.guideon.kiosk.client.CorePlaceClient;
import com.guideon.kiosk.client.FastApiQaClient;
import com.guideon.kiosk.domain.chat.dto.ChatMessageRequest;
import com.guideon.kiosk.domain.chat.dto.ChatMessageResponse;
import com.guideon.kiosk.domain.chat.dto.CreateSessionResponse;
import com.guideon.kiosk.global.security.DeviceDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final FastApiQaClient fastApiQaClient;
    private final CoreDailyInfoClient coreDailyInfoClient;
    private final CorePlaceClient corePlaceClient;

    /**
     * 대화 세션 생성 — UUID만 반환 (DB 저장 X, FastAPI Redis가 관리)
     */
    public CreateSessionResponse createSession() {
        String sessionId = UUID.randomUUID().toString();
        log.info("Chat 세션 생성: sessionId={}", sessionId);
        return new CreateSessionResponse(sessionId);
    }

    /**
     * 메시지 처리: context 조립 → FastAPI QA 호출 → display hint 조립
     */
    public ChatMessageResponse sendMessage(
            String sessionId,
            ChatMessageRequest request,
            DeviceDetails device
    ) {
        // 1. 오늘 DailyInfo context 조립
        List<QaRequest.DailyInfoSummary> dailyInfoSummaries = buildDailyInfoContext(device.getSiteId());

        // 2. FastAPI QA 요청 조립
        QaRequest qaRequest = QaRequest.builder()
                .sessionId(sessionId)
                .siteId(device.getSiteId())
                .question(request.getMessage())
                .language(request.getLanguage())
                .deviceLocation(QaRequest.DeviceLocation.builder()
                        .latitude(device.getLatitude())
                        .longitude(device.getLongitude())
                        .build())
                .context(QaRequest.QaContext.builder()
                        .dailyInfos(dailyInfoSummaries)
                        .build())
                .build();

        // 3. FastAPI 호출 (실패 시 stub 응답)
        QaResponse qaResponse = callFastApi(qaRequest);

        // 4. Display hint 조립 (placeId가 있으면)
        ChatMessageResponse.DisplayHint displayHint = buildDisplayHint(device.getSiteId(), qaResponse.getPlaceId());

        return ChatMessageResponse.builder()
                .sessionId(sessionId)
                .answer(qaResponse.getAnswer())
                .emotion(qaResponse.getEmotion())
                .language(qaResponse.getLanguage())
                .display(displayHint)
                .build();
    }

    private List<QaRequest.DailyInfoSummary> buildDailyInfoContext(Long siteId) {
        try {
            List<DailyInfoDto> dailyInfos = coreDailyInfoClient.getDailyInfos(siteId, LocalDate.now());
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
            log.warn("FastAPI QA 호출 실패 — stub 응답 반환: {}", e.getMessage());
            return QaResponse.builder()
                    .answer("[AI 서비스 연결 중] 죄송합니다, 잠시 후 다시 시도해주세요.")
                    .placeId(null)
                    .emotion("THINKING")
                    .language(request.getLanguage() != null ? request.getLanguage() : "ko")
                    .build();
        }
    }

    private ChatMessageResponse.DisplayHint buildDisplayHint(Long siteId, Long placeId) {
        if (placeId == null) {
            return null;
        }

        try {
            PlaceDto place = corePlaceClient.getPlace(siteId, placeId);
            return ChatMessageResponse.DisplayHint.builder()
                    .type("PLACE")
                    .placeId(place.getPlaceId())
                    .placeName(place.getName())
                    .imageUrl(place.getImageUrl())
                    .latitude(place.getLatitude())
                    .longitude(place.getLongitude())
                    .build();
        } catch (Exception e) {
            log.warn("Place 조회 실패 (display hint 생략): placeId={}, error={}", placeId, e.getMessage());
            return null;
        }
    }
}
