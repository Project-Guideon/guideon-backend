package com.guideon.guideonbackend.domain.mascot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MascotImageUploadResponse {

    @JsonProperty("image_url")
    private String imageUrl;
}
