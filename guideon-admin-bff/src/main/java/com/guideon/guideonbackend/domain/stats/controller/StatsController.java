package com.guideon.guideonbackend.domain.stats.controller;

import com.guideon.common.response.ApiResponse;
import com.guideon.core.dto.chat.AnswerRateStatDto;
import com.guideon.core.dto.chat.HourlyTrafficStatDto;
import com.guideon.core.dto.chat.QuestionTypeStatDto;
import com.guideon.core.dto.chat.SiteTrafficTop5Dto;
import com.guideon.core.dto.device.DeviceStatusStatDto;
import com.guideon.guideonbackend.domain.stats.service.StatsService;
import com.guideon.guideonbackend.global.security.CustomAdminDetails;
import com.guideon.guideonbackend.global.trace.TraceIdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "통계", description = "통계 API")
@RestController
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @Operation(summary = "질문 유형 통계 조회", description = "사이트별 질문 유형(카테고리)별 통계를 조회합니다.")
    @GetMapping("/api/v1/admin/sites/{siteId}/stats/question-types")
    public ResponseEntity<ApiResponse<QuestionTypeStatDto>> getQuestionTypeStats(
            @PathVariable Long siteId,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest) {
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(statsService.getQuestionTypeStats(siteId, adminDetails), traceId));
    }

    @Operation(summary = "AI 답변 성공률 통계 조회", description = "사이트별 AI 답변 성공률을 조회합니다.")
    @GetMapping("/api/v1/admin/sites/{siteId}/stats/answer-rate")
    public ResponseEntity<ApiResponse<AnswerRateStatDto>> getAnswerRateStat(
            @PathVariable Long siteId,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest) {
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(statsService.getAnswerRateStat(siteId, adminDetails), traceId));
    }

    @Operation(summary = "시간대별 요청량 조회", description = "사이트별 오늘 시간대별 채팅 요청량을 조회합니다.")
    @GetMapping("/api/v1/admin/sites/{siteId}/stats/hourly-traffic")
    public ResponseEntity<ApiResponse<HourlyTrafficStatDto>> getHourlyTrafficStat(
            @PathVariable Long siteId,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest) {
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(statsService.getHourlyTrafficStat(siteId, adminDetails), traceId));
    }

    @Operation(summary = "관광지별 트래픽 Top 5 조회", description = "전체 관광지 중 채팅 트래픽 상위 5개를 조회합니다. PLATFORM_ADMIN 전용.")
    @GetMapping("/api/v1/admin/stats/traffic-top5")
    public ResponseEntity<ApiResponse<SiteTrafficTop5Dto>> getSiteTrafficTop5(
            HttpServletRequest httpRequest) {
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(statsService.getSiteTrafficTop5(), traceId));
    }

    @Operation(summary = "기기 상태 통계 조회", description = "사이트별 기기 상태(정상/점검/장애)를 조회합니다.")
    @GetMapping("/api/v1/admin/sites/{siteId}/stats/device-status")
    public ResponseEntity<ApiResponse<DeviceStatusStatDto>> getDeviceStatusStat(
            @PathVariable Long siteId,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest) {
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(statsService.getDeviceStatusStat(siteId, adminDetails), traceId));
    }
}
