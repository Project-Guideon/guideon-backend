package com.guideon.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 초대 생성 요청 Command
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInviteCommand {
    private Long siteId;
    private String email;
    private Long createdByAdminId;
    private int expireDays;
}
