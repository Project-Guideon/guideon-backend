package com.guideon.core.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Site 수정 요청 Command
 */
@Getter
@Builder
public class UpdateSiteCommand {
    private String name;
}
