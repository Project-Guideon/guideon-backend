package com.guideon.guideonbackend.client;

import com.guideon.core.dto.chat.AnswerRateStatDto;
import com.guideon.core.dto.chat.HourlyTrafficStatDto;
import com.guideon.core.dto.chat.QuestionTypeStatDto;
import com.guideon.core.dto.chat.SiteTrafficTop5Dto;
import com.guideon.core.dto.device.DeviceStatusStatDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "core-stats", url = "${core.service.url}")
public interface CoreStatsClient {

    @GetMapping("/internal/v1/stats/question-types")
    QuestionTypeStatDto getQuestionTypeStats(@RequestParam("siteId") Long siteId);

    @GetMapping("/internal/v1/stats/answer-rate")
    AnswerRateStatDto getAnswerRateStat(@RequestParam("siteId") Long siteId);

    @GetMapping("/internal/v1/stats/hourly-traffic")
    HourlyTrafficStatDto getHourlyTrafficStat(@RequestParam("siteId") Long siteId);

    @GetMapping("/internal/v1/stats/traffic-top5")
    SiteTrafficTop5Dto getSiteTrafficTop5();

    @GetMapping("/internal/v1/stats/device-status")
    DeviceStatusStatDto getDeviceStatusStat(@RequestParam("siteId") Long siteId);
}
