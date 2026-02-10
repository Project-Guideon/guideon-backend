package com.guideon.core.api;

import com.guideon.core.dto.CreatePlaceCommand;
import com.guideon.core.dto.PlaceDto;
import com.guideon.core.dto.UpdatePlaceCommand;
import com.guideon.core.service.PlaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Place Internal API Controller
 * BFF에서 Feign Client로 호출하는 내부 API
 * 인증 없음 (내부망 전용)
 */
@RestController
@RequestMapping("/internal/v1/sites/{siteId}/places")
@RequiredArgsConstructor
public class PlaceInternalController {

    private final PlaceService placeService;

    @PostMapping
    public ResponseEntity<PlaceDto> createPlace(
            @PathVariable Long siteId,
            @RequestBody CreatePlaceCommand command) {
        PlaceDto place = placeService.createPlace(siteId, command);
        return ResponseEntity.ok(place);
    }

    @GetMapping
    public ResponseEntity<Page<PlaceDto>> getPlaces(
            @PathVariable Long siteId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PlaceDto> places = placeService.getPlaces(siteId, keyword, category, zoneId, isActive, pageable);
        return ResponseEntity.ok(places);
    }

    @GetMapping("/{placeId}")
    public ResponseEntity<PlaceDto> getPlace(
            @PathVariable Long siteId,
            @PathVariable Long placeId) {
        PlaceDto place = placeService.getPlace(siteId, placeId);
        return ResponseEntity.ok(place);
    }

    @PatchMapping("/{placeId}")
    public ResponseEntity<PlaceDto> updatePlace(
            @PathVariable Long siteId,
            @PathVariable Long placeId,
            @RequestBody UpdatePlaceCommand command) {
        PlaceDto place = placeService.updatePlace(siteId, placeId, command);
        return ResponseEntity.ok(place);
    }
}
