package com.guideon.guideonbackend.domain.document.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReprocessDocumentRequest {

    @JsonProperty("chunk_size")
    private Integer chunkSize;

    @JsonProperty("chunk_overlap")
    private Integer chunkOverlap;

    @JsonProperty("embedding_model")
    private String embeddingModel;
}
