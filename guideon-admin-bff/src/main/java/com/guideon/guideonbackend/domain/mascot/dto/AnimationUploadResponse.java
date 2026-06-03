package com.guideon.guideonbackend.domain.mascot.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class AnimationUploadResponse {

    private String animModelUrl;
    private Map<String, String> animClips;
}
