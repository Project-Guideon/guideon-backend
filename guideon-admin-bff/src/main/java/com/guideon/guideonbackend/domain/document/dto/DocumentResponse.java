package com.guideon.guideonbackend.domain.document.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.guideon.core.dto.DocumentDto;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DocumentResponse {

    @JsonProperty("doc_id")
    private Long docId;

    private String status;

    @JsonProperty("original_name")
    private String originalName;

    @JsonProperty("file_hash")
    private String fileHash;

    @JsonProperty("file_size")
    private Long fileSize;

    @JsonProperty("chunk_size")
    private Integer chunkSize;

    @JsonProperty("chunk_overlap")
    private Integer chunkOverlap;

    @JsonProperty("embedding_model")
    private String embeddingModel;

    @JsonProperty("failed_reason")
    private String failedReason;

    @JsonProperty("processed_at")
    private LocalDateTime processedAt;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    public static DocumentResponse from(DocumentDto dto) {
        return DocumentResponse.builder()
                .docId(dto.getDocId())
                .status(dto.getStatus())
                .originalName(dto.getOriginalName())
                .fileHash(dto.getFileHash())
                .fileSize(dto.getFileSize())
                .chunkSize(dto.getChunkSize())
                .chunkOverlap(dto.getChunkOverlap())
                .embeddingModel(dto.getEmbeddingModel())
                .failedReason(dto.getFailedReason())
                .processedAt(dto.getProcessedAt())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }
}
