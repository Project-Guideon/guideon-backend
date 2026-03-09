package com.guideon.guideonbackend.domain.device.dto;

import com.guideon.core.dto.RotateTokenResult;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateDeviceResponse {

    /** 일회성 평문 토큰 - 즉시 저장 필요, 이후 재조회 불가 */
    private String plainToken;
    private DeviceResponse device;

    public static CreateDeviceResponse from(RotateTokenResult result) {
        return CreateDeviceResponse.builder()
                .plainToken(result.getPlainToken())
                .device(DeviceResponse.from(result.getDevice()))
                .build();
    }
}
