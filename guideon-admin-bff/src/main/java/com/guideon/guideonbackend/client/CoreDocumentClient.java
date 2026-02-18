package com.guideon.guideonbackend.client;

import com.guideon.core.dto.CreateDocumentCommand;
import com.guideon.core.dto.DocumentDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "core-document", url = "${core.service.url}")
public interface CoreDocumentClient {

    @PostMapping("/internal/v1/sites/{siteId}/documents")
    DocumentDto createDocument(
            @PathVariable("siteId") Long siteId,
            @RequestBody CreateDocumentCommand command);
}
