package com.guideon.core.api;

import com.guideon.core.dto.chat.AnswerRateStatDto;
import com.guideon.core.dto.chat.HourlyTrafficStatDto;
import com.guideon.core.dto.chat.QuestionTypeStatDto;
import com.guideon.core.dto.chat.SiteTrafficTop5Dto;
import com.guideon.core.dto.device.DeviceStatusStatDto;
import com.guideon.core.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 통계 내부 API
 *
 * Admin BFF → Core: 대시보드 통계 조회
 */
@RestController
@RequestMapping("/internal/v1/stats")
@RequiredArgsConstructor
public class StatsInternalController {

    private final StatsService statsService;

    /**
     * GET /internal/v1/stats/question-types?siteId={siteId}
     * 사이트별 질문 유형 통계
     */
    @GetMapping("/question-types")
    public ResponseEntity<QuestionTypeStatDto> getQuestionTypeStats(@RequestParam Long siteId) {
        return ResponseEntity.ok(statsService.getQuestionTypeStats(siteId));
    }

    /**
     * GET /internal/v1/stats/answer-rate?siteId={siteId}
     * 사이트별 AI 답변 성공률
     */
    @GetMapping("/answer-rate")
    public ResponseEntity<AnswerRateStatDto> getAnswerRateStat(@RequestParam Long siteId) {
        return ResponseEntity.ok(statsService.getAnswerRateStat(siteId));
    }

    /**
     * GET /internal/v1/stats/hourly-traffic?siteId={siteId}
     * 사이트별 시간대별 요청량 (오늘 기준)
     */
    @GetMapping("/hourly-traffic")
    public ResponseEntity<HourlyTrafficStatDto> getHourlyTrafficStat(@RequestParam Long siteId) {
        return ResponseEntity.ok(statsService.getHourlyTrafficStat(siteId));
    }

    /**
     * GET /internal/v1/stats/traffic-top5
     * 전체 관광지 트래픽 Top 5 (PLATFORM_ADMIN 전용)
     */
    @GetMapping("/traffic-top5")
    public ResponseEntity<SiteTrafficTop5Dto> getSiteTrafficTop5() {
        return ResponseEntity.ok(statsService.getSiteTrafficTop5());
    }

    /**
     * GET /internal/v1/stats/device-status?siteId={siteId}
     * 사이트별 기기 상태 통계
     */
    @GetMapping("/device-status")
    public ResponseEntity<DeviceStatusStatDto> getDeviceStatusStat(@RequestParam Long siteId) {
        return ResponseEntity.ok(statsService.getDeviceStatusStat(siteId));
    }
}
