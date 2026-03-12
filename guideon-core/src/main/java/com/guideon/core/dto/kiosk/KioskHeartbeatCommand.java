package com.guideon.core.dto.kiosk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KioskHeartbeatCommand {
    private String version;    // 클라이언트 앱 버전 (nullable)
    private String errorCode;  // 기기 오류 코드 (nullable)
}