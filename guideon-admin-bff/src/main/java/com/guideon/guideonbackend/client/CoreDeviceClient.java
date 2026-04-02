package com.guideon.guideonbackend.client;

import com.guideon.common.response.PageResponse;
import com.guideon.core.dto.device.DeviceDto;
import com.guideon.core.dto.device.RotateTokenResult;
import com.guideon.core.dto.device.UpdateDeviceCommand;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "core-device", url = "${core.service.url}")
public interface CoreDeviceClient {

    @GetMapping("/internal/v1/sites/{siteId}/devices")
    PageResponse<DeviceDto> getDevices(
            @PathVariable("siteId") Long siteId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size);

    @GetMapping("/internal/v1/sites/{siteId}/devices/{deviceId}")
    DeviceDto getDevice(
            @PathVariable("siteId") Long siteId,
            @PathVariable("deviceId") String deviceId);

    @PatchMapping("/internal/v1/sites/{siteId}/devices/{deviceId}")
    DeviceDto updateDevice(
            @PathVariable("siteId") Long siteId,
            @PathVariable("deviceId") String deviceId,
            @RequestBody UpdateDeviceCommand command);

    @DeleteMapping("/internal/v1/sites/{siteId}/devices/{deviceId}")
    void deleteDevice(
            @PathVariable("siteId") Long siteId,
            @PathVariable("deviceId") String deviceId);

    @PostMapping("/internal/v1/sites/{siteId}/devices/{deviceId}/rotate-token")
    RotateTokenResult rotateToken(
            @PathVariable("siteId") Long siteId,
            @PathVariable("deviceId") String deviceId);
}
