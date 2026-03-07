package com.guideon.core.client;

import com.guideon.core.dto.ProcessDocumentCommand;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * FastAPI 문서 처리 Feign 클라이언트
 * Core → FastAPI: 청킹·임베딩 처리 요청
 */
@FeignClient(name = "fastapi-document", url = "${fastapi.service.url}")
public interface FastApiDocumentClient {

    @PostMapping("/v1/documents/process")
    void processDocument(@RequestBody ProcessDocumentCommand command);
}
