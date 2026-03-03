package com.guideon.guideonbackend.domain.place.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlaceImageUploadResponse {

    @JsonProperty("image_url")
    private String imageUrl;
}
