package com.guideon.core.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Site 생성 요청 Command
 */
@Getter
@Builder
public class CreateSiteCommand {
    private String name;
}
