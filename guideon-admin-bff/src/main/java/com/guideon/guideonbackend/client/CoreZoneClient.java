package com.guideon.guideonbackend.client;

import com.guideon.core.dto.CreateZoneCommand;
import com.guideon.core.dto.DeleteZoneResult;
import com.guideon.core.dto.ZoneDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * Core Service Zone API 호출용 Feign Client
 */
@FeignClient(name = "core-zone", url = "${core.service.url}")
public interface CoreZoneClient {

    @PostMapping("/internal/v1/sites/{siteId}/zones")
    ZoneDto createZone(
            @PathVariable("siteId") Long siteId,
            @RequestBody CreateZoneCommand command);

    @GetMapping("/internal/v1/sites/{siteId}/zones")
    Page<ZoneDto> getZones(
            @PathVariable("siteId") Long siteId,
            @RequestParam(value = "zoneType", required = false) String zoneType,
            @RequestParam(value = "parentZoneId", required = false) Long parentZoneId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size);

    @GetMapping("/internal/v1/sites/{siteId}/zones/{zoneId}")
    ZoneDto getZone(
            @PathVariable("siteId") Long siteId,
            @PathVariable("zoneId") Long zoneId);

    @DeleteMapping("/internal/v1/sites/{siteId}/zones/{zoneId}")
    DeleteZoneResult deleteZone(
            @PathVariable("siteId") Long siteId,
            @PathVariable("zoneId") Long zoneId);
}
