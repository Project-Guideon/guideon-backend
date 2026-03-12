package com.guideon.core.dto.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * FastAPI 문서 처리 요청 커맨드
 * Core → FastAPI: 처리 요청 시 사용 (chunk/model 파라미터는 FastAPI 내부 고정값 사용)
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

    public static ProcessDocumentCommand from(DocumentDto doc) {
        return ProcessDocumentCommand.builder()
                .docId(doc.getDocId())
                .siteId(doc.getSiteId())
                .storageUrl(doc.getStorageUrl())
                .build();
    }
}
