package com.guideon.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 초대 수락 요청 Command
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcceptInviteCommand {
    private String inviteToken;
    private String password;
}
