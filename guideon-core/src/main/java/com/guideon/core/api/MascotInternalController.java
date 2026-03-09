package com.guideon.core.api;

import com.guideon.core.dto.CreateMascotCommand;
import com.guideon.core.dto.MascotDto;
import com.guideon.core.dto.UpdateMascotCommand;
import com.guideon.core.service.MascotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/v1/sites/{siteId}/mascot")
@RequiredArgsConstructor
public class MascotInternalController {

    private final MascotService mascotService;

    @PostMapping
    public ResponseEntity<MascotDto> createMascot(
            @PathVariable Long siteId,
            @RequestBody CreateMascotCommand command) {
        return ResponseEntity.ok(mascotService.createMascot(siteId, command));
    }

    @GetMapping
    public ResponseEntity<MascotDto> getMascot(@PathVariable Long siteId) {
        return ResponseEntity.ok(mascotService.getMascot(siteId));
    }

    @PatchMapping
    public ResponseEntity<MascotDto> updateMascot(
            @PathVariable Long siteId,
            @RequestBody UpdateMascotCommand command) {
        return ResponseEntity.ok(mascotService.updateMascot(siteId, command));
    }
}
