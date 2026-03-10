package com.guideon.core.dto;

import com.guideon.core.domain.dailyinfo.entity.DailyInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyInfoDto {

    private Long id;
    private Long siteId;
    private Long placeId;
    private String placeName;
    private LocalDate targetDate;
    private String infoType;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DailyInfoDto from(DailyInfo dailyInfo) {
        return DailyInfoDto.builder()
                .id(dailyInfo.getId())
                .siteId(dailyInfo.getSiteId())
                .placeId(dailyInfo.getPlace().getPlaceId())
                .placeName(dailyInfo.getPlace().getName())
                .targetDate(dailyInfo.getTargetDate())
                .infoType(dailyInfo.getInfoType())
                .content(dailyInfo.getContent())
                .createdAt(dailyInfo.getCreatedAt())
                .updatedAt(dailyInfo.getUpdatedAt())
                .build();
    }
}