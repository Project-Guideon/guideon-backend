package com.guideon.guideonbackend.domain.site.dto;

import com.guideon.core.domain.site.entity.Site;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SiteResponse {

    private Long siteId;
    private String name;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SiteResponse from(Site site) {
        return SiteResponse.builder()
                .siteId(site.getSiteId())
                .name(site.getName())
                .isActive(site.getIsActive())
                .createdAt(site.getCreatedAt())
                .updatedAt(site.getUpdatedAt())
                .build();
    }
}
