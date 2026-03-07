package com.guideon.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FastAPI → Core: 문서 처리 결과 상태 업데이트 커맨드
 * status: COMPLETED 또는 FAILED
 * FAILED 시 failed_reason 필수
 */
@Getter
@NoArgsConstructor
public class UpdateDocumentStatusCommand {

    private String status;

    @JsonProperty("failed_reason")
    private String failedReason;
}
