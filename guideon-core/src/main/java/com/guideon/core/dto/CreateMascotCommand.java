package com.guideon.core.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@Getter
@Builder
@Jacksonized
public class CreateMascotCommand {

    private String name;
    private String modelId;
    private String defaultAnim;
    private String greetingMsg;
    private String systemPrompt;
    private Map<String, Object> promptConfig;
    private String ttsVoiceId;
    private Map<String, Object> ttsVoiceJson;
}
