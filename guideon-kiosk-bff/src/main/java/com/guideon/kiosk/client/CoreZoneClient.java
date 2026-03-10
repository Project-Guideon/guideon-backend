package com.guideon.kiosk.client;

import com.guideon.common.response.PageResponse;
import com.guideon.core.dto.ZoneDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "core-zone", url = "${core.service.url}")
public interface CoreZoneClient {

    @GetMapping("/internal/v1/sites/{siteId}/zones")
    PageResponse<ZoneDto> getZones(
            @PathVariable Long siteId,
            @RequestParam(required = false) String zoneType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size);
}