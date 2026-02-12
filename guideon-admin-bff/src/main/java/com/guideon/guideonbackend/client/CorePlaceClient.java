package com.guideon.guideonbackend.client;

import com.guideon.core.dto.CreatePlaceCommand;
import com.guideon.core.dto.PlaceDto;
import com.guideon.core.dto.UpdatePlaceCommand;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
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

    @GetMapping("/internal/v1/sites/{siteId}/places")
    Page<PlaceDto> getPlaces(
            @PathVariable("siteId") Long siteId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "zoneId", required = false) Long zoneId,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size);

    @GetMapping("/internal/v1/sites/{siteId}/places/{placeId}")
    PlaceDto getPlace(
            @PathVariable("siteId") Long siteId,
            @PathVariable("placeId") Long placeId);

    @PatchMapping("/internal/v1/sites/{siteId}/places/{placeId}")
    PlaceDto updatePlace(
            @PathVariable("siteId") Long siteId,
            @PathVariable("placeId") Long placeId,
            @RequestBody UpdatePlaceCommand command);

    @DeleteMapping("/internal/v1/sites/{siteId}/places/{placeId}")
    void deletePlace(
            @PathVariable("siteId") Long siteId,
            @PathVariable("placeId") Long placeId);
}
