package com.guideon.core.dto;

import com.guideon.core.domain.site.entity.Site;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Core 내부 통신용 Site DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiteDto {

    private Long siteId;
    private String name;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SiteDto from(Site site) {
        return SiteDto.builder()
                .siteId(site.getSiteId())
                .name(site.getName())
                .isActive(site.getIsActive())
                .createdAt(site.getCreatedAt())
                .updatedAt(site.getUpdatedAt())
                .build();
    }
}
