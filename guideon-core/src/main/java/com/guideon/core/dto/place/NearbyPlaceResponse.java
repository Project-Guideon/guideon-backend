package com.guideon.core.dto.place;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NearbyPlaceResponse {

    private Long placeId;
    private String name;
    private String category;
    private String description;
    private Double distanceM;
    private boolean sameZone;
}
