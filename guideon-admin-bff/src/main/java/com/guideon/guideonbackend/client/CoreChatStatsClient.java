package com.guideon.guideonbackend.client;

import com.guideon.core.dto.chat.QuestionTypeStatDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "core-chat-stats", url = "${core.service.url}")
public interface CoreChatStatsClient {

    @GetMapping("/internal/v1/chat/stats/question-types")
    QuestionTypeStatDto getQuestionTypeStats(@RequestParam("siteId") Long siteId);
}
