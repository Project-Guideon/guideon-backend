package com.guideon.core.api;

import com.guideon.common.response.PageResponse;
import com.guideon.core.client.FastApiDocumentService;
import com.guideon.core.dto.document.CreateDocumentCommand;
import com.guideon.core.dto.document.DocumentDto;
import com.guideon.core.dto.document.ProcessDocumentCommand;
import com.guideon.core.dto.document.ReprocessDocumentCommand;
import com.guideon.core.dto.document.UpdateDocumentStatusCommand;
import com.guideon.core.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/v1/sites/{siteId}/documents")
@RequiredArgsConstructor
public class DocumentInternalController {

    private final DocumentService documentService;
    private final FastApiDocumentService fastApiDocumentService;

    @PostMapping
    public ResponseEntity<DocumentDto> createDocument(
            @PathVariable Long siteId,
            @RequestBody CreateDocumentCommand command) {
        DocumentDto document = documentService.createDocument(siteId, command);
        // 트랜잭션 커밋 후 FastAPI에 비동기 처리 요청
        fastApiDocumentService.processDocument(ProcessDocumentCommand.from(document));
        return ResponseEntity.ok(document);
    }

    @GetMapping
    public ResponseEntity<PageResponse<DocumentDto>> getDocuments(
            @PathVariable Long siteId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(
                documentService.getDocuments(siteId, keyword, status, pageable)));
    }

    @GetMapping("/{docId}")
    public ResponseEntity<DocumentDto> getDocument(
            @PathVariable Long siteId,
            @PathVariable Long docId) {
        DocumentDto document = documentService.getDocument(siteId, docId);
        return ResponseEntity.ok(document);
    }

    @PostMapping("/{docId}/reprocess")
    public ResponseEntity<DocumentDto> reprocessDocument(
            @PathVariable Long siteId,
            @PathVariable Long docId,
            @RequestBody(required = false) ReprocessDocumentCommand command) {
        DocumentDto document = documentService.reprocessDocument(siteId, docId);
        // 트랜잭션 커밋 후 FastAPI에 비동기 재처리 요청
        fastApiDocumentService.processDocument(ProcessDocumentCommand.from(document));
        return ResponseEntity.ok(document);
    }

    @PatchMapping("/{docId}/status")
    public ResponseEntity<DocumentDto> updateDocumentStatus(
            @PathVariable Long siteId,
            @PathVariable Long docId,
            @RequestBody @Valid UpdateDocumentStatusCommand command) {
        DocumentDto document = documentService.updateDocumentStatus(siteId, docId, command);
        return ResponseEntity.ok(document);
    }

    @DeleteMapping("/{docId}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long siteId,
            @PathVariable Long docId) {
        documentService.deleteDocument(siteId, docId);
        return ResponseEntity.noContent().build();
    }
}
