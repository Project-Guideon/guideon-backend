package com.guideon.kiosk.client;

import com.guideon.core.dto.QaRequest;
import com.guideon.core.dto.QaResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "fastapi-qa", url = "${fastapi.service.url}")
public interface FastApiQaClient {

    @PostMapping("/internal/v1/qa")
    QaResponse ask(@RequestBody QaRequest request);
}
