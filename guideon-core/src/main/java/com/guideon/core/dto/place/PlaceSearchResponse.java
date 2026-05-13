package com.guideon.core.dto.place;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlaceSearchResponse {

    private Long placeId;
    private String name;
    private String category;
    private Double latitude;
    private Double longitude;
    private Double similarity;
}
