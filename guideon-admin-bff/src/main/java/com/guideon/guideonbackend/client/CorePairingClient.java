package com.guideon.guideonbackend.client;

import com.guideon.core.dto.device.DeviceDto;
import com.guideon.core.dto.pairing.PairDeviceCommand;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "core-pairing", url = "${core.service.url}")
public interface CorePairingClient {

    @PostMapping("/internal/v1/pairing/sites/{siteId}/pair")
    DeviceDto pairDevice(
            @PathVariable("siteId") Long siteId,
            @RequestBody PairDeviceCommand command);
}
