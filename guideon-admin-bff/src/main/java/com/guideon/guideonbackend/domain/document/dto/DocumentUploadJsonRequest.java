package com.guideon.guideonbackend.domain.document.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DocumentUploadJsonRequest {

    @NotBlank(message = "파일명은 필수입니다")
    @JsonProperty("original_name")
    private String originalName;

    @NotBlank(message = "파일 데이터는 필수입니다")
    @JsonProperty("file_base64")
    private String fileBase64;

    @JsonProperty("chunk_size")
    private Integer chunkSize;

    @JsonProperty("chunk_overlap")
    private Integer chunkOverlap;

    @JsonProperty("embedding_model")
    private String embeddingModel;
}
