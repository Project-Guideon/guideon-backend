package com.guideon.kiosk.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {

    @NotBlank(message = "message는 필수입니다")
    private String message;

    private String language;  // STT에서 감지된 언어 (nullable → FastAPI auto-detect)
}
