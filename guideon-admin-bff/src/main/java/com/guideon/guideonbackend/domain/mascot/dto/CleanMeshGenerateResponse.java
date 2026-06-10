package com.guideon.guideonbackend.domain.mascot.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CleanMeshGenerateResponse {

    /** Tripo image_to_model taskId — 프론트가 폴링에 사용 */
    private String taskId;
}
