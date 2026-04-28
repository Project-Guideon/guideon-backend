package com.guideon.kiosk.client;

import com.guideon.core.dto.dailyinfo.DailyInfoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "core-daily-info", url = "${core.service.url}")
public interface CoreDailyInfoClient {

    @GetMapping("/internal/v1/sites/{siteId}/daily-infos")
    List<DailyInfoDto> getDailyInfos(@PathVariable Long siteId);
}
