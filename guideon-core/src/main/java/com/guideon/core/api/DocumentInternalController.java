package com.guideon.core.api;

import com.guideon.common.response.PageResponse;
import com.guideon.core.dto.CreateDocumentCommand;
import com.guideon.core.dto.DocumentDto;
import com.guideon.core.dto.ReprocessDocumentCommand;
import com.guideon.core.service.DocumentService;
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

    @PostMapping
    public ResponseEntity<DocumentDto> createDocument(
            @PathVariable Long siteId,
            @RequestBody CreateDocumentCommand command) {
        DocumentDto document = documentService.createDocument(siteId, command);
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
            @RequestBody ReprocessDocumentCommand command) {
        DocumentDto document = documentService.reprocessDocument(siteId, docId, command);
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
