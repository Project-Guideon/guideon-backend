package com.guideon.guideonbackend.client;

import com.guideon.core.dto.CreateMascotCommand;
import com.guideon.core.dto.MascotDto;
import com.guideon.core.dto.UpdateMascotCommand;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "core-mascot", url = "${core.service.url}")
public interface CoreMascotClient {

    @PostMapping("/internal/v1/sites/{siteId}/mascot")
    MascotDto createMascot(
            @PathVariable("siteId") Long siteId,
            @RequestBody CreateMascotCommand command);

    @GetMapping("/internal/v1/sites/{siteId}/mascot")
    MascotDto getMascot(@PathVariable("siteId") Long siteId);

    @PatchMapping("/internal/v1/sites/{siteId}/mascot")
    MascotDto updateMascot(
            @PathVariable("siteId") Long siteId,
            @RequestBody UpdateMascotCommand command);
}
