package com.guideon.kiosk.client;

import com.guideon.core.dto.SiteDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "core-site", url = "${core.service.url}")
public interface CoreSiteClient {

    @GetMapping("/internal/v1/sites/{siteId}")
    SiteDto getSite(@PathVariable Long siteId);
}