package com.guideon.core.dto;

import com.guideon.core.domain.place.entity.Place;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Core 내부 통신용 Place DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceDto {

    private Long placeId;
    private Long siteId;
    private Long zoneId;
    private String zoneSource;
    private String name;
    private Map<String, String> nameJson;
    private String category;
    private Double latitude;
    private Double longitude;
    private String description;
    private String imageUrl;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PlaceDto from(Place place) {
        return PlaceDto.builder()
                .placeId(place.getPlaceId())
                .siteId(place.getSite().getSiteId())
                .zoneId(place.getZone() != null ? place.getZone().getZoneId() : null)
                .zoneSource(place.getZoneSource().name())
                .name(place.getName())
                .nameJson(place.getNameJson())
                .category(place.getCategory())
                .latitude(place.getLocation().getY())
                .longitude(place.getLocation().getX())
                .description(place.getDescription())
                .imageUrl(place.getImageUrl())
                .isActive(place.getIsActive())
                .createdAt(place.getCreatedAt())
                .updatedAt(place.getUpdatedAt())
                .build();
    }
}
