package com.guideon.guideonbackend.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminChatMessageRequest {

    @NotNull
    private Long siteId;

    @NotBlank
    private String deviceId;

    @NotBlank
    private String message;

    private String language = "ko-KR";

    private Double latitude;
    private Double longitude;
}
