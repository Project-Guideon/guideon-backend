package com.guideon.core.api;

import com.guideon.core.dto.CreateZoneCommand;
import com.guideon.core.dto.DeleteZoneResult;
import com.guideon.core.dto.UpdateZoneCommand;
import com.guideon.core.dto.ZoneDto;
import com.guideon.core.service.ZoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Zone Internal API Controller
 * BFF에서 Feign Client로 호출하는 내부 API
 * 인증 없음 (내부망 전용)
 */
@RestController
@RequestMapping("/internal/v1/sites/{siteId}/zones")
@RequiredArgsConstructor
public class ZoneInternalController {

    private final ZoneService zoneService;

    @PostMapping
    public ResponseEntity<ZoneDto> createZone(
            @PathVariable Long siteId,
            @RequestBody CreateZoneCommand command) {
        ZoneDto zone = zoneService.createZone(siteId, command);
        return ResponseEntity.ok(zone);
    }

    @GetMapping
    public ResponseEntity<Page<ZoneDto>> getZones(
            @PathVariable Long siteId,
            @RequestParam(required = false) String zoneType,
            @RequestParam(required = false) Long parentZoneId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ZoneDto> zones = zoneService.getZones(siteId, zoneType, parentZoneId, pageable);
        return ResponseEntity.ok(zones);
    }

    @GetMapping("/{zoneId}")
    public ResponseEntity<ZoneDto> getZone(
            @PathVariable Long siteId,
            @PathVariable Long zoneId) {
        ZoneDto zone = zoneService.getZone(siteId, zoneId);
        return ResponseEntity.ok(zone);
    }

    @PatchMapping("/{zoneId}")
    public ResponseEntity<ZoneDto> updateZone(
            @PathVariable Long siteId,
            @PathVariable Long zoneId,
            @RequestBody UpdateZoneCommand command) {
        ZoneDto zone = zoneService.updateZone(siteId, zoneId, command);
        return ResponseEntity.ok(zone);
    }

    @DeleteMapping("/{zoneId}")
    public ResponseEntity<DeleteZoneResult> deleteZone(
            @PathVariable Long siteId,
            @PathVariable Long zoneId) {
        DeleteZoneResult result = zoneService.deleteZone(siteId, zoneId);
        return ResponseEntity.ok(result);
    }
}
