package com.guideon.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RotateTokenResult {

    /** 일회성 평문 토큰 - 즉시 저장 필요, 이후 재조회 불가 */
    private String plainToken;
    private DeviceDto device;
}
