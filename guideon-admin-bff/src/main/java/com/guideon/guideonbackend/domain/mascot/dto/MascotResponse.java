package com.guideon.guideonbackend.domain.mascot.dto;

import com.guideon.core.dto.mascot.MascotDto;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
public class MascotResponse {

    private Long mascotId;
    private Long siteId;
    private String name;
    private String modelId;
    private String defaultAnim;
    private String greetingMsg;
    private String systemPrompt;
    private Map<String, Object> promptConfig;
    private String ttsVoiceId;
    private Map<String, Object> ttsVoiceJson;
    private String imageUrl;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MascotResponse from(MascotDto dto) {
        return MascotResponse.builder()
                .mascotId(dto.getMascotId())
                .siteId(dto.getSiteId())
                .name(dto.getName())
                .modelId(dto.getModelId())
                .defaultAnim(dto.getDefaultAnim())
                .greetingMsg(dto.getGreetingMsg())
                .systemPrompt(dto.getSystemPrompt())
                .promptConfig(dto.getPromptConfig())
                .ttsVoiceId(dto.getTtsVoiceId())
                .ttsVoiceJson(dto.getTtsVoiceJson())
                .imageUrl(dto.getImageUrl())
                .isActive(dto.getIsActive())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }
}
