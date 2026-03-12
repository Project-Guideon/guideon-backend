package com.guideon.core.api;

import com.guideon.core.dto.mascot.CreateMascotCommand;
import com.guideon.core.dto.mascot.MascotDto;
import com.guideon.core.dto.mascot.UpdateMascotCommand;
import com.guideon.core.service.MascotService;
import jakarta.validation.Valid;
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
            @Valid @RequestBody CreateMascotCommand command) {
        return ResponseEntity.ok(mascotService.createMascot(siteId, command));
    }

    @GetMapping
    public ResponseEntity<MascotDto> getMascot(@PathVariable Long siteId) {
        return ResponseEntity.ok(mascotService.getMascot(siteId));
    }

    @PatchMapping
    public ResponseEntity<MascotDto> updateMascot(
            @PathVariable Long siteId,
            @Valid @RequestBody UpdateMascotCommand command) {
        return ResponseEntity.ok(mascotService.updateMascot(siteId, command));
    }
}
