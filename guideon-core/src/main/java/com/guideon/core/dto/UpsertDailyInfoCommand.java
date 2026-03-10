package com.guideon.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpsertDailyInfoCommand {
    private Long placeId;
    private String infoType;
    private String content;
}