package com.guideon.core.client;

import com.guideon.core.dto.qa.QaRequest;
import com.guideon.core.dto.qa.QaResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * FastAPI QA Feign 클라이언트
 * Core → FastAPI: RAG + LLM 질의응답
 */
@FeignClient(name = "fastapi-qa", url = "${fastapi.service.url}")
public interface FastApiQaClient {

    @PostMapping("/internal/v1/qa")
    QaResponse ask(@RequestBody QaRequest request);
}
