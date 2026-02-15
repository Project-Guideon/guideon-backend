package com.guideon.core.api;

import com.guideon.core.dto.CreateDocumentCommand;
import com.guideon.core.dto.DocumentDto;
import com.guideon.core.service.DocumentService;
import lombok.RequiredArgsConstructor;
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
}
