package com.guideon.guideonbackend.domain.site.dto;

import com.guideon.core.dto.site.SiteDto;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SiteResponse {

    private Long siteId;
    private String name;
    private Boolean isActive;
    private Double latitude;
    private Double longitude;
    private Integer mapLevel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SiteResponse from(SiteDto dto) {
        return SiteResponse.builder()
                .siteId(dto.getSiteId())
                .name(dto.getName())
                .isActive(dto.getIsActive())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .mapLevel(dto.getMapLevel())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }
}
