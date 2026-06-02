package com.guideon.core.dto.kiosk;

import com.guideon.core.domain.mascot.entity.Mascot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KioskMascotDto {

    private String name;
    private String modelId;
    private String imageUrl;
    private String modelUrl;
    private String modelFormat;
    private String defaultAnim;
    private String greetingMsg;
    private String systemPrompt;
    private Map<String, Object> promptConfig;
    private String ttsVoiceId;
    private Map<String, Object> ttsVoiceJson;
    private String animModelUrl;           // 5클립 내장 GLB URL (없으면 프로시저럴 폴백)
    private Map<String, String> animClips; // 상태→클립명 (Unity Animation.Play 키)

    public static KioskMascotDto from(Mascot mascot) {
        return KioskMascotDto.builder()
                .name(mascot.getName())
                .modelId(mascot.getModelId())
                .imageUrl(mascot.getImageUrl())
                .modelUrl(mascot.getModelUrl())
                .modelFormat(mascot.getModelFormat())
                .defaultAnim(mascot.getDefaultAnim())
                .greetingMsg(mascot.getGreetingMsg())
                .systemPrompt(mascot.getSystemPrompt())
                .promptConfig(mascot.getPromptConfig())
                .ttsVoiceId(mascot.getTtsVoiceId())
                .ttsVoiceJson(mascot.getTtsVoiceJson())
                .animModelUrl(mascot.getAnimModelUrl())
                .animClips(mascot.getAnimClips())
                .build();
    }
}
