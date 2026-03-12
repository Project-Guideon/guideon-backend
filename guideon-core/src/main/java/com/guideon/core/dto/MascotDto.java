package com.guideon.core.dto;

import com.guideon.core.domain.mascot.entity.Mascot;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
public class MascotDto {

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

    public static MascotDto from(Mascot mascot) {
        return MascotDto.builder()
                .mascotId(mascot.getMascotId())
                .siteId(mascot.getSite().getSiteId())
                .name(mascot.getName())
                .modelId(mascot.getModelId())
                .defaultAnim(mascot.getDefaultAnim())
                .greetingMsg(mascot.getGreetingMsg())
                .systemPrompt(mascot.getSystemPrompt())
                .promptConfig(mascot.getPromptConfig())
                .ttsVoiceId(mascot.getTtsVoiceId())
                .ttsVoiceJson(mascot.getTtsVoiceJson())
                .imageUrl(mascot.getImageUrl())
                .isActive(mascot.getIsActive())
                .createdAt(mascot.getCreatedAt())
                .updatedAt(mascot.getUpdatedAt())
                .build();
    }
}
