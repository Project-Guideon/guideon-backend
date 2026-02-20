package com.guideon.guideonbackend.domain.document.service;

import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import com.guideon.core.domain.admin.entity.AdminRole;
import com.guideon.core.domain.admin.repository.AdminSiteRepository;
import com.guideon.core.dto.CreateDocumentCommand;
import com.guideon.core.dto.DocumentDto;
import com.guideon.core.dto.ReprocessDocumentCommand;
import com.guideon.guideonbackend.client.CoreDocumentClient;
import com.guideon.guideonbackend.domain.document.dto.DocumentResponse;
import com.guideon.guideonbackend.domain.document.dto.ReprocessDocumentRequest;
import com.guideon.guideonbackend.global.security.CustomAdminDetails;
import com.guideon.guideonbackend.global.storage.FileStorageService;
import com.guideon.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;

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
        validateFileType(file);

        String fileHash = computeFileHash(file);
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
        String fileHash = computeFileHash(fileBytes);
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

        Page<DocumentDto> documentPage = coreDocumentClient.getDocuments(
                siteId, keyword, status,
                pageable.getPageNumber(), pageable.getPageSize());

        return PageResponse.from(documentPage.map(DocumentResponse::from));
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
        coreDocumentClient.deleteDocument(siteId, docId);
        log.info("문서 삭제 완료: docId={}, siteId={}", docId, siteId);
    }

    private String computeFileHash(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192]; // 8KB 버퍼
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) { // 8KB씩 읽기
                digest.update(buffer, 0, bytesRead); // 읽은 만큼만 해시에 추가
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new CustomException(ErrorCode.DOC_UPLOAD_FAILED,
                    "파일 해시 계산에 실패했습니다: " + e.getMessage());
        }
    }

    private String computeFileHash(byte[] fileBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(fileBytes);
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new CustomException(ErrorCode.DOC_UPLOAD_FAILED,
                    "파일 해시 계산에 실패했습니다: " + e.getMessage());
        }
    }

    private void validateFileType(MultipartFile file) {
        String contentType = file.getContentType();
        String originalName = file.getOriginalFilename();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new CustomException(ErrorCode.DOC_UPLOAD_FAILED, "PDF 파일만 업로드 가능합니다.");
        }
        if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) {
            throw new CustomException(ErrorCode.DOC_UPLOAD_FAILED, "PDF 파일만 업로드 가능합니다.");
        }
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
