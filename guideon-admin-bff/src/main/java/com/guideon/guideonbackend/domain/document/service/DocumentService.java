package com.guideon.guideonbackend.domain.document.service;

import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import com.guideon.core.domain.admin.entity.AdminRole;
import com.guideon.core.domain.admin.repository.AdminSiteRepository;
import com.guideon.common.response.PageResponse;
import com.guideon.core.dto.CreateDocumentCommand;
import com.guideon.core.dto.DocumentDto;
import com.guideon.core.dto.ReprocessDocumentCommand;
import com.guideon.guideonbackend.client.CoreDocumentClient;
import com.guideon.guideonbackend.domain.document.dto.DocumentResponse;
import com.guideon.guideonbackend.domain.document.dto.ReprocessDocumentRequest;
import com.guideon.guideonbackend.global.security.CustomAdminDetails;
import com.guideon.guideonbackend.global.storage.FileStorageService;
import com.guideon.guideonbackend.global.storage.FileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final FileStorageService fileStorageService;
    private final CoreDocumentClient coreDocumentClient;
    private final AdminSiteRepository adminSiteRepository;

    /**
     * 문서 업로드 (multipart)
     */
    public DocumentResponse uploadDocument(Long siteId, MultipartFile file,
                                           Integer chunkSize, Integer chunkOverlap,
                                           String embeddingModel,
                                           CustomAdminDetails adminDetails) {
        validateSiteAccess(adminDetails, siteId);
        FileValidator.validatePdf(file);

        String fileHash = FileValidator.computeFileHash(file);
        String storageUrl = fileStorageService.store(siteId, fileHash, file);

        CreateDocumentCommand command = CreateDocumentCommand.builder()
                .originalName(file.getOriginalFilename())
                .storageUrl(storageUrl)
                .fileHash(fileHash)
                .fileSize(file.getSize())
                .chunkSize(chunkSize)
                .chunkOverlap(chunkOverlap)
                .embeddingModel(embeddingModel)
                .build();

        DocumentDto documentDto = coreDocumentClient.createDocument(siteId, command);
        log.info("문서 업로드 완료: docId={}, siteId={}, originalName={}",
                documentDto.getDocId(), siteId, file.getOriginalFilename());

        return DocumentResponse.from(documentDto);
    }

    /**
     * 문서 업로드 (JSON base64) - 테스트 전용
     */
    public DocumentResponse uploadDocumentJson(Long siteId, String originalName,
                                               String fileBase64,
                                               Integer chunkSize, Integer chunkOverlap,
                                               String embeddingModel,
                                               CustomAdminDetails adminDetails) {
        validateSiteAccess(adminDetails, siteId);

        byte[] fileBytes = Base64.getDecoder().decode(fileBase64);
        String fileHash = FileValidator.computeFileHash(fileBytes);
        String storageUrl = fileStorageService.store(siteId, fileHash, fileBytes, originalName);

        CreateDocumentCommand command = CreateDocumentCommand.builder()
                .originalName(originalName)
                .storageUrl(storageUrl)
                .fileHash(fileHash)
                .fileSize((long) fileBytes.length)
                .chunkSize(chunkSize)
                .chunkOverlap(chunkOverlap)
                .embeddingModel(embeddingModel)
                .build();

        DocumentDto documentDto = coreDocumentClient.createDocument(siteId, command);
        log.info("문서 업로드 완료 (base64): docId={}, siteId={}, originalName={}",
                documentDto.getDocId(), siteId, originalName);

        return DocumentResponse.from(documentDto);
    }

    public PageResponse<DocumentResponse> getDocuments(Long siteId, String keyword, String status,
                                                       Pageable pageable, CustomAdminDetails adminDetails) {
        validateSiteAccess(adminDetails, siteId);

        List<String> sortParam = convertSortToList(pageable.getSort());

        PageResponse<DocumentDto> documentPage = coreDocumentClient.getDocuments(
                siteId, keyword, status,
                pageable.getPageNumber(), pageable.getPageSize(), sortParam);

        return PageResponse.<DocumentResponse>builder()
                .items(documentPage.getItems().stream().map(DocumentResponse::from).toList())
                .page(documentPage.getPage())
                .build();
    }

    public DocumentResponse getDocument(Long siteId, Long docId, CustomAdminDetails adminDetails) {
        validateSiteAccess(adminDetails, siteId);
        DocumentDto documentDto = coreDocumentClient.getDocument(siteId, docId);
        return DocumentResponse.from(documentDto);
    }

    public DocumentResponse reprocessDocument(Long siteId, Long docId,
                                               ReprocessDocumentRequest request,
                                               CustomAdminDetails adminDetails) {
        validateSiteAccess(adminDetails, siteId);

        ReprocessDocumentCommand command = ReprocessDocumentCommand.builder()
                .chunkSize(request.getChunkSize())
                .chunkOverlap(request.getChunkOverlap())
                .embeddingModel(request.getEmbeddingModel())
                .build();

        DocumentDto documentDto = coreDocumentClient.reprocessDocument(siteId, docId, command);
        log.info("문서 재처리 요청 완료: docId={}, siteId={}", docId, siteId);
        return DocumentResponse.from(documentDto);
    }

    public void deleteDocument(Long siteId, Long docId, CustomAdminDetails adminDetails) {
        validateSiteAccess(adminDetails, siteId);
        DocumentDto documentDto = coreDocumentClient.getDocument(siteId, docId);
        coreDocumentClient.deleteDocument(siteId, docId);
        try {
            fileStorageService.delete(documentDto.getStorageUrl());
        } catch (Exception e) {
            log.error("문서 파일 삭제 실패 (DB는 삭제됨): docId={}, storageUrl={}, error={}",
                    docId, documentDto.getStorageUrl(), e.getMessage());
        }
        log.info("문서 삭제 완료: docId={}, siteId={}", docId, siteId);
    }

    // camelCase(API) → snake_case(native query 컬럼명) 매핑
    private static final Map<String, String> DOCUMENT_SORT_FIELD_MAP = Map.of(
            "docId",        "doc_id",
            "originalName", "original_name",
            "status",       "status",
            "createdAt",    "created_at",
            "updatedAt",    "updated_at"
    );

    /**
     * Spring Sort 객체를 ["doc_id,desc", "original_name,asc"] 형태의 List로 변환
     * 클라이언트가 camelCase(?sort=docId)로 전송 → native query용 snake_case로 변환
     * Feign이 ?sort=doc_id,desc&sort=original_name,asc 으로 직렬화함
     * 유효하지 않은 필드명은 무시
     */
    private List<String> convertSortToList(Sort sort) {
        if (sort.isUnsorted()) return null;

        List<String> result = sort.stream()
                .filter(order -> DOCUMENT_SORT_FIELD_MAP.containsKey(order.getProperty()))
                .map(order -> DOCUMENT_SORT_FIELD_MAP.get(order.getProperty()) + "," + order.getDirection().name().toLowerCase())
                .collect(Collectors.toList());

        return result.isEmpty() ? null : result;
    }

    private void validateSiteAccess(CustomAdminDetails adminDetails, Long siteId) {
        if (AdminRole.SITE_ADMIN.name().equals(adminDetails.getRole())) {
            if (!adminSiteRepository.existsById_AdminIdAndId_SiteId(
                    adminDetails.getAdminId(), siteId)) {
                throw new CustomException(ErrorCode.ADMIN_SITE_FORBIDDEN);
            }
        }
    }
}
