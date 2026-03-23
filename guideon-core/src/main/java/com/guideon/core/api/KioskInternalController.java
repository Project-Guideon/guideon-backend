package com.guideon.core.api;

import com.guideon.core.dto.kiosk.KioskBootstrapDto;
import com.guideon.core.dto.kiosk.KioskHeartbeatCommand;
import com.guideon.core.dto.kiosk.KioskMascotDto;
import com.guideon.core.service.KioskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/v1/kiosk/devices/{deviceId}")
@RequiredArgsConstructor
public class KioskInternalController {

    private final KioskService kioskService;

    @GetMapping("/bootstrap")
    public ResponseEntity<KioskBootstrapDto> getBootstrap(@PathVariable String deviceId) {
        return ResponseEntity.ok(kioskService.getBootstrap(deviceId));
    }

    @GetMapping("/mascot")
    public ResponseEntity<KioskMascotDto> getMascot(@PathVariable String deviceId) {
        return ResponseEntity.ok(kioskService.getMascot(deviceId));
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<Void> heartbeat(
            @PathVariable String deviceId,
            @RequestBody KioskHeartbeatCommand command) {
        kioskService.heartbeat(deviceId, command);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auth")
    public ResponseEntity<Void> updateLastAuthAt(@PathVariable String deviceId) {
        kioskService.updateLastAuthAt(deviceId);
        return ResponseEntity.noContent().build();
    }
}