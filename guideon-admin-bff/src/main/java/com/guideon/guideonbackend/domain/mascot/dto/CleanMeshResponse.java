package com.guideon.guideonbackend.domain.mascot.dto;

import lombok.Builder;
import lombok.Getter;

/** GET /mascot/clean-mesh 응답 */
@Getter
@Builder
public class CleanMeshResponse {

    /** 스켈레톤 제거된 T-포즈 FBX URL. Mixamo 업로드 후 애니메이션 다운로드에 사용. null이면 미생성. */
    private String cleanMeshUrl;

    /** "ready" | "not_available" */
    private String status;
}
