package com.guideon.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpsertDailyInfoCommand {
    @NotNull(message = "placeId는 필수입니다")
    private Long placeId;

    @NotBlank(message = "infoType은 필수입니다")
    private String infoType;

    @NotBlank(message = "content는 필수입니다")
    private String content;
}