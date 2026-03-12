package com.guideon.guideonbackend.domain.mascot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.Map;

@Getter
public class CreateMascotRequest {

    @NotBlank
    @Size(max = 50)
    private String name;

    @NotBlank
    @Size(max = 80)
    private String modelId;

    @Size(max = 50)
    private String defaultAnim;

    @NotBlank
    @Size(max = 200)
    private String greetingMsg;

    @NotBlank
    @Size(max = 2000, message = "systemPrompt는 2000자 이하여야 합니다")
    private String systemPrompt;

    private Map<String, Object> promptConfig;

    @NotBlank
    @Size(max = 50)
    private String ttsVoiceId;

    private Map<String, Object> ttsVoiceJson;

    @Size(max = 500)
    private String imageUrl;
}
