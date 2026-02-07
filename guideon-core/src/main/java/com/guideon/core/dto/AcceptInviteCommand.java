package com.guideon.core.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 초대 수락 요청 Command
 */
@Getter
@Builder
public class AcceptInviteCommand {
    private String inviteToken;
    private String password;
}
