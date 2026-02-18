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

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    public static DocumentResponse from(DocumentDto dto) {
        return DocumentResponse.builder()
                .docId(dto.getDocId())
                .status(dto.getStatus())
                .originalName(dto.getOriginalName())
                .fileHash(dto.getFileHash())
                .createdAt(dto.getCreatedAt())
                .build();
    }
}
