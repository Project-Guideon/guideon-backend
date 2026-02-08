package com.guideon.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Site 수정 요청 Command
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSiteCommand {
    private String name;
}
