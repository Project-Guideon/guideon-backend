package com.guideon.core.api;

import com.guideon.common.response.PageResponse;
import com.guideon.core.dto.*;
import com.guideon.core.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/v1/sites/{siteId}/devices")
@RequiredArgsConstructor
public class DeviceInternalController {

    private final DeviceService deviceService;

    @PostMapping
    public ResponseEntity<RotateTokenResult> createDevice(
            @PathVariable Long siteId,
            @RequestBody CreateDeviceCommand command) {
        return ResponseEntity.ok(deviceService.createDevice(siteId, command));
    }

    @GetMapping
    public ResponseEntity<PageResponse<DeviceDto>> getDevices(
            @PathVariable Long siteId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(deviceService.getDevices(siteId, pageable)));
    }

    @GetMapping("/{deviceId}")
    public ResponseEntity<DeviceDto> getDevice(
            @PathVariable Long siteId,
            @PathVariable String deviceId) {
        return ResponseEntity.ok(deviceService.getDevice(siteId, deviceId));
    }

    @PatchMapping("/{deviceId}")
    public ResponseEntity<DeviceDto> updateDevice(
            @PathVariable Long siteId,
            @PathVariable String deviceId,
            @RequestBody UpdateDeviceCommand command) {
        return ResponseEntity.ok(deviceService.updateDevice(siteId, deviceId, command));
    }

    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> deleteDevice(
            @PathVariable Long siteId,
            @PathVariable String deviceId) {
        deviceService.deleteDevice(siteId, deviceId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{deviceId}/rotate-token")
    public ResponseEntity<RotateTokenResult> rotateToken(
            @PathVariable Long siteId,
            @PathVariable String deviceId) {
        return ResponseEntity.ok(deviceService.rotateToken(siteId, deviceId));
    }
}
