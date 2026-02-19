package com.guideon.core.service;

import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import com.guideon.core.domain.document.entity.Document;
import com.guideon.core.domain.document.repository.DocumentRepository;
import com.guideon.core.domain.site.entity.Site;
import com.guideon.core.domain.site.repository.SiteRepository;
import com.guideon.core.dto.CreateDocumentCommand;
import com.guideon.core.dto.DocumentDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final SiteRepository siteRepository;

    @Transactional
    public DocumentDto createDocument(Long siteId, CreateDocumentCommand command) {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND,
                        "존재하지 않는 관광지입니다: " + siteId));

        if (!site.getIsActive()) {
            throw new CustomException(ErrorCode.SITE_INACTIVE);
        }

        // 파일 해시 중복 검사
        if (documentRepository.existsBySite_SiteIdAndFileHash(siteId, command.getFileHash())) {
            throw new CustomException(ErrorCode.DOC_HASH_DUPLICATE);
        }

        Document document = Document.builder()
                .site(site)
                .originalName(command.getOriginalName())
                .storageUrl(command.getStorageUrl())
                .fileHash(command.getFileHash())
                .fileSize(command.getFileSize())
                .chunkSize(command.getChunkSize())
                .chunkOverlap(command.getChunkOverlap())
                .embeddingModel(command.getEmbeddingModel())
                .build();

        Document saved;
        try {
            saved = documentRepository.save(document);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.DOC_HASH_DUPLICATE);
        }
        log.info("문서 메타데이터 저장 완료: docId={}, siteId={}, originalName={}",
                saved.getDocId(), siteId, command.getOriginalName());

        return DocumentDto.from(saved);
    }

    public Page<DocumentDto> getDocuments(Long siteId, String keyword, String status, Pageable pageable) {
        return documentRepository.findByFilters(siteId, keyword, status, pageable)
                .map(DocumentDto::from);
    }

    public DocumentDto getDocument(Long siteId, Long docId) {
        Document document = documentRepository.findByDocIdAndSite_SiteId(docId, siteId)
                .orElseThrow(() -> new CustomException(ErrorCode.DOC_NOT_FOUND));
        return DocumentDto.from(document);
    }

    @Transactional
    public void deleteDocument(Long siteId, Long docId) {
        Document document = documentRepository.findByDocIdAndSite_SiteId(docId, siteId)
                .orElseThrow(() -> new CustomException(ErrorCode.DOC_NOT_FOUND));
        documentRepository.delete(document);
        log.info("문서 삭제 완료: docId={}, siteId={}", docId, siteId);
    }
}
