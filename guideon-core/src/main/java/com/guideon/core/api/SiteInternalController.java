package com.guideon.core.api;

import com.guideon.core.dto.CreateSiteCommand;
import com.guideon.core.dto.SiteDto;
import com.guideon.core.dto.UpdateSiteCommand;
import com.guideon.core.service.SiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Site Internal API Controller
 * BFF에서 Feign Client로 호출하는 내부 API
 * 인증 없음 (내부망 전용)
 */
@RestController
@RequestMapping("/internal/v1/sites")
@RequiredArgsConstructor
public class SiteInternalController {

    private final SiteService siteService;

    @PostMapping
    public ResponseEntity<SiteDto> createSite(@RequestBody CreateSiteCommand command) {
        SiteDto site = siteService.createSite(command);
        return ResponseEntity.ok(site);
    }

    @GetMapping
    public ResponseEntity<List<SiteDto>> getAllSites() {
        List<SiteDto> sites = siteService.getAllSites();
        return ResponseEntity.ok(sites);
    }

    @GetMapping("/{siteId}")
    public ResponseEntity<SiteDto> getSite(@PathVariable Long siteId) {
        SiteDto site = siteService.getSite(siteId);
        return ResponseEntity.ok(site);
    }

    @PatchMapping("/{siteId}")
    public ResponseEntity<SiteDto> updateSite(
            @PathVariable Long siteId,
            @RequestBody UpdateSiteCommand command) {
        SiteDto site = siteService.updateSite(siteId, command);
        return ResponseEntity.ok(site);
    }

    @PostMapping("/{siteId}/deactivate")
    public ResponseEntity<Void> deactivateSite(@PathVariable Long siteId) {
        siteService.deactivateSite(siteId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{siteId}/activate")
    public ResponseEntity<Void> activateSite(@PathVariable Long siteId) {
        siteService.activateSite(siteId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{siteId}/exists")
    public ResponseEntity<Boolean> existsById(@PathVariable Long siteId) {
        return ResponseEntity.ok(siteService.existsById(siteId));
    }
}
