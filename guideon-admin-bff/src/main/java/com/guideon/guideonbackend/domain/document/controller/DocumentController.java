package com.guideon.guideonbackend.domain.document.controller;

import com.guideon.common.response.ApiResponse;
import com.guideon.guideonbackend.domain.document.dto.DocumentResponse;
import com.guideon.guideonbackend.domain.document.dto.DocumentUploadJsonRequest;
import com.guideon.guideonbackend.domain.document.service.DocumentService;
import com.guideon.guideonbackend.global.security.CustomAdminDetails;
import com.guideon.guideonbackend.global.trace.TraceIdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "문서 관리", description = "문서(Document) 업로드 API")
@RestController
@RequestMapping("/api/v1/admin/sites/{siteId}/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @Operation(summary = "문서 업로드 (multipart)",
            description = "PDF 파일을 multipart/form-data로 업로드합니다.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @PathVariable Long siteId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "chunk_size", required = false) Integer chunkSize,
            @RequestParam(value = "chunk_overlap", required = false) Integer chunkOverlap,
            @RequestParam(value = "embedding_model", required = false) String embeddingModel,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        DocumentResponse response = documentService.uploadDocument(
                siteId, file, chunkSize, chunkOverlap, embeddingModel, adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    @Operation(summary = "문서 업로드 (JSON base64)",
            description = "파일을 base64로 인코딩하여 JSON으로 업로드합니다. 테스트 전용.")
    @PostMapping("/upload-json")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocumentJson(
            @PathVariable Long siteId,
            @Valid @RequestBody DocumentUploadJsonRequest request,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        DocumentResponse response = documentService.uploadDocumentJson(
                siteId, request.getOriginalName(), request.getFileBase64(),
                request.getChunkSize(), request.getChunkOverlap(),
                request.getEmbeddingModel(), adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }
}
