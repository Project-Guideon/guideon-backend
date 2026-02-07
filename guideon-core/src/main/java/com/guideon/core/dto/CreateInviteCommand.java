package com.guideon.core.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 초대 생성 요청 Command
 */
@Getter
@Builder
public class CreateInviteCommand {
    private Long siteId;
    private String email;
    private Long createdByAdminId;
    private int expireDays;
}
