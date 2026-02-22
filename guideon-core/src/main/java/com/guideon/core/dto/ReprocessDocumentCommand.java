package com.guideon.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReprocessDocumentCommand {

    private Integer chunkSize;
    private Integer chunkOverlap;
    private String embeddingModel;
}
