package com.guideon.guideonbackend.client;

import com.guideon.core.dto.admin.OperatorDto;
import com.guideon.core.dto.admin.UpdateOperatorCommand;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "core-operator", url = "${core.service.url}")
public interface CoreOperatorClient {

    @GetMapping("/internal/v1/sites/{siteId}/operators")
    List<OperatorDto> getSiteOperators(@PathVariable("siteId") Long siteId);

    @PatchMapping("/internal/v1/sites/{siteId}/operators/{operatorId}")
    OperatorDto updateOperatorStatus(
            @PathVariable("siteId") Long siteId,
            @PathVariable("operatorId") Long operatorId,
            @RequestBody UpdateOperatorCommand command);
}
