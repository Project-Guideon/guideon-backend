package com.guideon.guideonbackend.domain.document.controller;

import com.guideon.common.response.ApiResponse;
import com.guideon.common.response.PageResponse;
import com.guideon.guideonbackend.domain.document.dto.DocumentResponse;
import com.guideon.guideonbackend.domain.document.dto.DocumentUploadJsonRequest;
import com.guideon.guideonbackend.domain.document.dto.ReprocessDocumentRequest;
import com.guideon.guideonbackend.domain.document.service.DocumentService;
import com.guideon.guideonbackend.global.security.CustomAdminDetails;
import com.guideon.guideonbackend.global.trace.TraceIdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "문서 관리", description = "문서(Document) 관리 API")
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
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        DocumentResponse response = documentService.uploadDocument(siteId, file, adminDetails);
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
                siteId, request.getOriginalName(), request.getFileBase64(), adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    @Operation(summary = "문서 목록 조회", description = "업로드된 문서 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DocumentResponse>>> getDocuments(
            @PathVariable Long siteId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        PageResponse<DocumentResponse> response = documentService.getDocuments(
                siteId, keyword, status, pageable, adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    @Operation(summary = "문서 상세 조회", description = "특정 문서의 상세 정보를 조회합니다.")
    @GetMapping("/{docId}")
    public ResponseEntity<ApiResponse<DocumentResponse>> getDocument(
            @PathVariable Long siteId,
            @PathVariable Long docId,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        DocumentResponse response = documentService.getDocument(siteId, docId, adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    @Operation(summary = "문서 재처리", description = "실패한 문서를 재처리합니다.")
    @PostMapping("/{docId}/reprocess")
    public ResponseEntity<ApiResponse<DocumentResponse>> reprocessDocument(
            @PathVariable Long siteId,
            @PathVariable Long docId,
            @RequestBody(required = false) ReprocessDocumentRequest request,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        DocumentResponse response = documentService.reprocessDocument(siteId, docId, adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(response, traceId));
    }

    @Operation(summary = "문서 삭제", description = "업로드된 문서를 삭제합니다.")
    @DeleteMapping("/{docId}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable Long siteId,
            @PathVariable Long docId,
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            HttpServletRequest httpRequest
    ) {
        documentService.deleteDocument(siteId, docId, adminDetails);
        String traceId = (String) httpRequest.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return ResponseEntity.ok(ApiResponse.success(null, traceId));
    }
}
