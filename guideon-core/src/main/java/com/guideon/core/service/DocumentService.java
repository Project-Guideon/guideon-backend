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

        Document saved = documentRepository.save(document);
        log.info("문서 메타데이터 저장 완료: docId={}, siteId={}, originalName={}",
                saved.getDocId(), siteId, command.getOriginalName());

        return DocumentDto.from(saved);
    }
}
