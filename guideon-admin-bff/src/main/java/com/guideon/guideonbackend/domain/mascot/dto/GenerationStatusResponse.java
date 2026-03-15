package com.guideon.guideonbackend.domain.mascot.dto;

import com.guideon.core.domain.mascot.entity.MascotGeneration;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class GenerationStatusResponse {

    private Long generationId;
    private String sourceImageUrl;
    private String modelTaskId;
    private String modelStatus;
    private String rigTaskId;
    private String rigStatus;
    private String resultModelUrl;
    private String failedReason;
    private boolean completed;
    private boolean failed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static GenerationStatusResponse from(MascotGeneration gen) {
        return GenerationStatusResponse.builder()
                .generationId(gen.getGenerationId())
                .sourceImageUrl(gen.getSourceImageUrl())
                .modelTaskId(gen.getModelTaskId())
                .modelStatus(gen.getModelStatusValue())
                .rigTaskId(gen.getRigTaskId())
                .rigStatus(gen.getRigStatusValue())
                .resultModelUrl(gen.getResultModelUrl())
                .failedReason(gen.getFailedReason())
                .completed(gen.isFullyCompleted())
                .failed(gen.isFailed())
                .createdAt(gen.getCreatedAt())
                .updatedAt(gen.getUpdatedAt())
                .build();
    }
}
