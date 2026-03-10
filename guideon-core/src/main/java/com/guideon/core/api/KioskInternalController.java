package com.guideon.core.api;

import com.guideon.core.dto.KioskBootstrapDto;
import com.guideon.core.dto.KioskHeartbeatCommand;
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