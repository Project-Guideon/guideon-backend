package com.guideon.guideonbackend.domain.stats.controller;

import com.guideon.common.response.ApiResponse;
import com.guideon.core.dto.chat.QuestionTypeStatDto;
import com.guideon.guideonbackend.client.CoreChatStatsClient;
import com.guideon.guideonbackend.global.trace.TraceIdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "통계", description = "통계 API")
@RestController
@RequestMapping("/api/v1/admin/sites/{siteId}/stats")
@RequiredArgsConstructor
public class StatsController {

    private final CoreChatStatsClient coreChatStatsClient;

    @Operation(summary = "질문 유형 통계 조회", description = "사이트별 질문 유형(카테고리)별 통계를 조회합니다.")
    @GetMapping("/question-types")
    public ResponseEntity<ApiResponse<QuestionTypeStatDto>> getQuestionTypeStats(
            @PathVariable Long siteId,
            HttpServletRequest httpRequest
    ) {
        QuestionTypeStatDto result = coreChatStatsClient.getQuestionTypeStats(siteId);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(result, traceId));
    }
}
