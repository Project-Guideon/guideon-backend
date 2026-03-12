package com.guideon.kiosk.domain.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class HeartbeatRequest {
    private String version;
    private String errorCode;
}