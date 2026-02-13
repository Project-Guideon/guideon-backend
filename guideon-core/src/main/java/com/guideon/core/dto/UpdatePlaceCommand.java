package com.guideon.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePlaceCommand {
    private String name;
    private String category;
    private String description;
    private String imageUrl;
    private Double latitude;
    private Double longitude;
    private Boolean isActive;
    private String zoneSource;  // MANUAL일 때만 zone 변경
    private Long zoneId;        // MANUAL + zoneId로 고정 가능
}
