package com.guideon.core.dto.mascot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@Getter
@Builder
@Jacksonized
public class CreateMascotCommand {

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
    @Size(max = 2000)
    private String systemPrompt;

    private Map<String, Object> promptConfig;

    @NotBlank
    @Size(max = 50)
    private String ttsVoiceId;

    private Map<String, Object> ttsVoiceJson;

    @Size(max = 500)
    private String imageUrl;
}
