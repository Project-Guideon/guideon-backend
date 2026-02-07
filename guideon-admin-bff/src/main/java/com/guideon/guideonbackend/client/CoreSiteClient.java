package com.guideon.guideonbackend.client;

import com.guideon.core.dto.CreateSiteCommand;
import com.guideon.core.dto.SiteDto;
import com.guideon.core.dto.UpdateSiteCommand;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Core Service Site API 호출용 Feign Client
 */
@FeignClient(name = "core-site", url = "${core.service.url}")
public interface CoreSiteClient {

    @PostMapping("/internal/v1/sites")
    SiteDto createSite(@RequestBody CreateSiteCommand command);

    @GetMapping("/internal/v1/sites")
    List<SiteDto> getAllSites();

    @GetMapping("/internal/v1/sites/{siteId}")
    SiteDto getSite(@PathVariable("siteId") Long siteId);

    @PatchMapping("/internal/v1/sites/{siteId}")
    SiteDto updateSite(@PathVariable("siteId") Long siteId, @RequestBody UpdateSiteCommand command);

    @PostMapping("/internal/v1/sites/{siteId}/deactivate")
    void deactivateSite(@PathVariable("siteId") Long siteId);

    @PostMapping("/internal/v1/sites/{siteId}/activate")
    void activateSite(@PathVariable("siteId") Long siteId);

    @GetMapping("/internal/v1/sites/{siteId}/exists")
    Boolean existsById(@PathVariable("siteId") Long siteId);
}
