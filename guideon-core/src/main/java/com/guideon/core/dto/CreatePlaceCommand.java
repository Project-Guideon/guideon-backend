package com.guideon.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Place 생성 요청 Command
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePlaceCommand {
    private String name;
    private Map<String, String> nameJson;
    private String category;
    private Double latitude;
    private Double longitude;
    private String description;
    private String imageUrl;
    private Boolean isActive;
    private String zoneSource;  // AUTO / MANUAL
}
