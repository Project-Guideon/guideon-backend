package com.guideon.core.api;

import com.guideon.core.dto.CreateDocumentCommand;
import com.guideon.core.dto.DocumentDto;
import com.guideon.core.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
    public ResponseEntity<Page<DocumentDto>> getDocuments(
            @PathVariable Long siteId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<DocumentDto> documents = documentService.getDocuments(siteId, keyword, status, pageable);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{docId}")
    public ResponseEntity<DocumentDto> getDocument(
            @PathVariable Long siteId,
            @PathVariable Long docId) {
        DocumentDto document = documentService.getDocument(siteId, docId);
        return ResponseEntity.ok(document);
    }
}
