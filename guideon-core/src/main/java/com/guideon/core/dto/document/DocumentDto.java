package com.guideon.core.dto.document;

import com.guideon.core.domain.document.entity.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDto {

    private Long docId;
    private Long siteId;
    private String originalName;
    private String storageUrl;
    private String fileHash;
    private Long fileSize;
    private Integer chunkSize;
    private Integer chunkOverlap;
    private String embeddingModel;
    private String status;
    private String failedReason;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DocumentDto from(Document doc) {
        return DocumentDto.builder()
                .docId(doc.getDocId())
                .siteId(doc.getSite().getSiteId())
                .originalName(doc.getOriginalName())
                .storageUrl(doc.getStorageUrl())
                .fileHash(doc.getFileHash())
                .fileSize(doc.getFileSize())
                .chunkSize(doc.getChunkSize())
                .chunkOverlap(doc.getChunkOverlap())
                .embeddingModel(doc.getEmbeddingModel())
                .status(doc.getStatus().name())
                .failedReason(doc.getFailedReason())
                .processedAt(doc.getProcessedAt())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
