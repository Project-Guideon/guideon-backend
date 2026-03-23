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
    private String ttsVoiceId;
    private Map<String, Object> ttsVoiceJson;

    public static KioskMascotDto from(Mascot mascot) {
        return KioskMascotDto.builder()
                .name(mascot.getName())
                .modelId(mascot.getModelId())
                .imageUrl(mascot.getImageUrl())
                .modelUrl(mascot.getModelUrl())
                .modelFormat(mascot.getModelFormat())
                .defaultAnim(mascot.getDefaultAnim())
                .greetingMsg(mascot.getGreetingMsg())
                .ttsVoiceId(mascot.getTtsVoiceId())
                .ttsVoiceJson(mascot.getTtsVoiceJson())
                .build();
    }
}
