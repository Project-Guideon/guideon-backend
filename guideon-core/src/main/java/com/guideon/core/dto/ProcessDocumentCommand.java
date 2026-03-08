package com.guideon.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * FastAPI 문서 처리 요청 커맨드
 * Core → FastAPI: 청킹·임베딩 처리 요청 시 사용
 */
@Getter
@Builder
public class ProcessDocumentCommand {

    @JsonProperty("doc_id")
    private Long docId;

    @JsonProperty("site_id")
    private Long siteId;

    @JsonProperty("storage_url")
    private String storageUrl;

    @JsonProperty("chunk_size")
    private Integer chunkSize;

    @JsonProperty("chunk_overlap")
    private Integer chunkOverlap;

    @JsonProperty("embedding_model")
    private String embeddingModel;

    public static ProcessDocumentCommand from(DocumentDto doc) {
        return ProcessDocumentCommand.builder()
                .docId(doc.getDocId())
                .siteId(doc.getSiteId())
                .storageUrl(doc.getStorageUrl())
                .chunkSize(doc.getChunkSize())
                .chunkOverlap(doc.getChunkOverlap())
                .embeddingModel(doc.getEmbeddingModel())
                .build();
    }
}
