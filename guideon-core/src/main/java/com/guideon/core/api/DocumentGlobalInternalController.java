package com.guideon.core.api;

import com.guideon.core.dto.document.DocumentDto;
import com.guideon.core.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/v1/documents")
@RequiredArgsConstructor
public class DocumentGlobalInternalController {

    private final DocumentService documentService;

    /** PENDING 상태로 지정 시간 이상 방치된 문서 조회 (Admin BFF 재처리 스케줄러용) */
    @GetMapping("/pending")
    public ResponseEntity<List<DocumentDto>> getPendingDocuments(
            @RequestParam(defaultValue = "5") int olderThanMinutes) {
        return ResponseEntity.ok(documentService.getPendingDocumentsOlderThan(olderThanMinutes));
    }
}
