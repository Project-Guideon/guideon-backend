package com.guideon.core.dto.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDocumentCommand {
    private String originalName;
    private String storageUrl;
    private String fileHash;
    private Long fileSize;
}
