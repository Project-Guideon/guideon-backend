package com.guideon.guideonbackend.client;

import com.guideon.common.response.PageResponse;
import com.guideon.core.dto.CreateDocumentCommand;
import com.guideon.core.dto.DocumentDto;
import com.guideon.core.dto.ReprocessDocumentCommand;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "core-document", url = "${core.service.url}")
public interface CoreDocumentClient {

    @PostMapping("/internal/v1/sites/{siteId}/documents")
    DocumentDto createDocument(
            @PathVariable("siteId") Long siteId,
            @RequestBody CreateDocumentCommand command);

    @GetMapping("/internal/v1/sites/{siteId}/documents")
    PageResponse<DocumentDto> getDocuments(
            @PathVariable("siteId") Long siteId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size);

    @GetMapping("/internal/v1/sites/{siteId}/documents/{docId}")
    DocumentDto getDocument(
            @PathVariable("siteId") Long siteId,
            @PathVariable("docId") Long docId);

    @PostMapping("/internal/v1/sites/{siteId}/documents/{docId}/reprocess")
    DocumentDto reprocessDocument(
            @PathVariable("siteId") Long siteId,
            @PathVariable("docId") Long docId,
            @RequestBody ReprocessDocumentCommand command);

    @DeleteMapping("/internal/v1/sites/{siteId}/documents/{docId}")
    void deleteDocument(
            @PathVariable("siteId") Long siteId,
            @PathVariable("docId") Long docId);
}
