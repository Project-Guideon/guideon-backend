package com.guideon.core.dto.stt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FastAPI STT 응답 DTO
 *
 * FastAPI → Kiosk BFF
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SttResponse {
    private String transcript;
    private boolean isFinal;       // true=최종 결과, false=중간 결과
    private double confidence;
}
