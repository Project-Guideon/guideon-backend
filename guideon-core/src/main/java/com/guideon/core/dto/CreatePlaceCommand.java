package com.guideon.core.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Place 생성 요청 Command
 */
@Getter
@Builder
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
