package com.guideon.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

/**
 * FastAPI → Core: 문서 처리 결과 상태 업데이트 커맨드
 * status: PROCESSING, COMPLETED, FAILED 중 하나
 * FAILED 시 failed_reason 필수
 */
@Getter
@NoArgsConstructor
public class UpdateDocumentStatusCommand {

    @NotBlank(message = "status는 필수입니다.")
    private String status;

    @JsonProperty("failed_reason")
    private String failedReason;

    @AssertTrue(message = "FAILED 상태에서는 failed_reason이 필요합니다.")
    public boolean isFailedReasonValid() {
        return !"FAILED".equals(status) || StringUtils.hasText(failedReason);
    }
}
