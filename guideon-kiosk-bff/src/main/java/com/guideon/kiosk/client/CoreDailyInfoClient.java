package com.guideon.kiosk.client;

import com.guideon.core.dto.DailyInfoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "core-daily-info", url = "${core.service.url}")
public interface CoreDailyInfoClient {

    @GetMapping("/internal/v1/sites/{siteId}/daily-infos")
    List<DailyInfoDto> getDailyInfos(
            @PathVariable Long siteId,
            @RequestParam(required = false) String date);
}