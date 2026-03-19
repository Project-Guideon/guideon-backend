package com.guideon.kiosk.client;

import com.guideon.core.dto.kiosk.KioskBootstrapDto;
import com.guideon.core.dto.kiosk.KioskHeartbeatCommand;
import com.guideon.core.dto.kiosk.KioskMascotDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "core-kiosk", url = "${core.service.url}")
public interface CoreKioskClient {

    @GetMapping("/internal/v1/kiosk/devices/{deviceId}/bootstrap")
    KioskBootstrapDto getBootstrap(@PathVariable String deviceId);

    @GetMapping("/internal/v1/kiosk/devices/{deviceId}/mascot")
    KioskMascotDto getMascot(@PathVariable String deviceId);

    @PostMapping("/internal/v1/kiosk/devices/{deviceId}/heartbeat")
    void heartbeat(@PathVariable String deviceId, @RequestBody KioskHeartbeatCommand command);

    @PostMapping("/internal/v1/kiosk/devices/{deviceId}/auth")
    void updateLastAuthAt(@PathVariable String deviceId);
}
