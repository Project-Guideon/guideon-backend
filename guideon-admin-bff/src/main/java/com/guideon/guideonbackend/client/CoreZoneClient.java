package com.guideon.guideonbackend.client;

import com.guideon.common.response.PageResponse;
import com.guideon.core.dto.zone.CreateZoneCommand;
import com.guideon.core.dto.zone.DeleteZoneResult;
import com.guideon.core.dto.zone.RecalcResultDto;
import com.guideon.core.dto.zone.UpdateZoneCommand;
import com.guideon.core.dto.zone.ZoneDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    PageResponse<ZoneDto> getZones(
            @PathVariable("siteId") Long siteId,
            @RequestParam(value = "zoneType", required = false) String zoneType,
            @RequestParam(value = "parentZoneId", required = false) Long parentZoneId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", required = false) List<String> sort);

    @GetMapping("/internal/v1/sites/{siteId}/zones/{zoneId}")
    ZoneDto getZone(
            @PathVariable("siteId") Long siteId,
            @PathVariable("zoneId") Long zoneId);

    @PatchMapping("/internal/v1/sites/{siteId}/zones/{zoneId}")
    ZoneDto updateZone(
            @PathVariable("siteId") Long siteId,
            @PathVariable("zoneId") Long zoneId,
            @RequestBody UpdateZoneCommand command);

    @DeleteMapping("/internal/v1/sites/{siteId}/zones/{zoneId}")
    DeleteZoneResult deleteZone(
            @PathVariable("siteId") Long siteId,
            @PathVariable("zoneId") Long zoneId);

    @PostMapping("/internal/v1/sites/{siteId}/zones/recalc")
    RecalcResultDto recalculateZones(@PathVariable("siteId") Long siteId);
}
