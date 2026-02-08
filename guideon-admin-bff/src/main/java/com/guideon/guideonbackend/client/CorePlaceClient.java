package com.guideon.guideonbackend.client;

import com.guideon.core.dto.CreatePlaceCommand;
import com.guideon.core.dto.PlaceDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Core Service Place API 호출용 Feign Client
 */
@FeignClient(name = "core-place", url = "${core.service.url}")
public interface CorePlaceClient {

    @PostMapping("/internal/v1/sites/{siteId}/places")
    PlaceDto createPlace(
            @PathVariable("siteId") Long siteId,
            @RequestBody CreatePlaceCommand command);
}
