package com.guideon.guideonbackend.domain.mascot.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.Map;

@Getter
public class UpdateMascotRequest {

    @Size(max = 50)
    private String name;

    @Size(max = 80)
    private String modelId;

    @Size(max = 50)
    private String defaultAnim;

    @Size(max = 200)
    private String greetingMsg;

    private String systemPrompt;

    private Map<String, Object> promptConfig;

    @Size(max = 50)
    private String ttsVoiceId;

    private Map<String, Object> ttsVoiceJson;

    @Size(max = 500)
    private String imageUrl;

    private Boolean isActive;
}
