package com.guideon.kiosk.client;

import com.guideon.common.response.PageResponse;
import com.guideon.core.dto.place.PlaceDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "core-place", url = "${core.service.url}")
public interface CorePlaceClient {

    @GetMapping("/internal/v1/sites/{siteId}/places")
    PageResponse<PlaceDto> getPlaces(
            @PathVariable Long siteId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size);

    @GetMapping("/internal/v1/sites/{siteId}/places/{placeId}")
    PlaceDto getPlace(@PathVariable Long siteId, @PathVariable Long placeId);
}