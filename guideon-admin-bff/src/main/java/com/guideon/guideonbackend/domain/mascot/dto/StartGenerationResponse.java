package com.guideon.guideonbackend.domain.mascot.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StartGenerationResponse {

    private Long generationId;
    private String modelTaskId;
    private String status;
}
