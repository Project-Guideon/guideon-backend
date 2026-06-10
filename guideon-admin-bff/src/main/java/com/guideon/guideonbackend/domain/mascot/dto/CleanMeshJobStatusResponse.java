package com.guideon.guideonbackend.domain.mascot.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CleanMeshJobStatusResponse {

    private String taskId;

    /** "processing" | "ready" | "failed" */
    private String status;

    /** ready 일 때만 non-null */
    private String cleanMeshUrl;
}
