package com.guideon.core.dto;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@Getter
@Builder
@Jacksonized
public class UpdateMascotCommand {

    @Size(max = 50)
    private String name;

    @Size(max = 80)
    private String modelId;

    @Size(max = 50)
    private String defaultAnim;

    @Size(max = 200)
    private String greetingMsg;

    @Size(max = 2000)
    private String systemPrompt;

    private Map<String, Object> promptConfig;

    @Size(max = 50)
    private String ttsVoiceId;

    private Map<String, Object> ttsVoiceJson;

    private Boolean isActive;
}
