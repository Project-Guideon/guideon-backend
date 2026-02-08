package com.guideon.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Site 생성 요청 Command
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSiteCommand {
    private String name;
}
