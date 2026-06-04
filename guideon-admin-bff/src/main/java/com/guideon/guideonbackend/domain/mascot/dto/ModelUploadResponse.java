package com.guideon.guideonbackend.domain.mascot.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ModelUploadResponse {

    /** 저장된 GLB URL (tb_mascot.model_url) */
    private String modelUrl;

    /** anim_config가 설정된 경우 자동 병합된 anim GLB URL, 미설정이면 null */
    private String animModelUrl;
}
