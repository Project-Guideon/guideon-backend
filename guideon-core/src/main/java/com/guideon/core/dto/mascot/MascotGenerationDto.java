package com.guideon.core.dto.mascot;

import com.guideon.core.domain.mascot.entity.MascotGeneration;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MascotGenerationDto {

    private Long generationId;
    private Long siteId;
    private String sourceImageUrl;
    private String modelTaskId;
    private String modelStatus;
    private String rigTaskId;
    private String rigStatus;
    private String resultModelUrl;
    private String failedReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MascotGenerationDto from(MascotGeneration gen) {
        return MascotGenerationDto.builder()
                .generationId(gen.getGenerationId())
                .siteId(gen.getSite().getSiteId())
                .sourceImageUrl(gen.getSourceImageUrl())
                .modelTaskId(gen.getModelTaskId())
                .modelStatus(gen.getModelStatusValue())
                .rigTaskId(gen.getRigTaskId())
                .rigStatus(gen.getRigStatusValue())
                .resultModelUrl(gen.getResultModelUrl())
                .failedReason(gen.getFailedReason())
                .createdAt(gen.getCreatedAt())
                .updatedAt(gen.getUpdatedAt())
                .build();
    }
}
