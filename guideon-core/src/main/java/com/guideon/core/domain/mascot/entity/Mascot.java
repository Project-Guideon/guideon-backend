package com.guideon.core.domain.mascot.entity;

import com.guideon.core.domain.site.entity.Site;
import com.guideon.core.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "tb_mascot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mascot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mascot_id")
    private Long mascotId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false, unique = true)
    private Site site;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "model_id", nullable = false, length = 80)
    private String modelId;

    @Column(name = "default_anim", nullable = false, length = 50)
    private String defaultAnim;

    @Column(name = "greeting_msg", nullable = false, length = 200)
    private String greetingMsg;

    @Column(name = "system_prompt", nullable = false, columnDefinition = "TEXT")
    private String systemPrompt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "prompt_config", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> promptConfig;

    @Column(name = "tts_voice_id", nullable = false, length = 50)
    private String ttsVoiceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tts_voice_json", columnDefinition = "jsonb")
    private Map<String, Object> ttsVoiceJson;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Builder
    public Mascot(Site site, String name, String modelId, String defaultAnim,
                  String greetingMsg, String systemPrompt, Map<String, Object> promptConfig,
                  String ttsVoiceId, Map<String, Object> ttsVoiceJson) {
        this.site = site;
        this.name = name;
        this.modelId = modelId;
        this.defaultAnim = defaultAnim != null ? defaultAnim : "IDLE_A";
        this.greetingMsg = greetingMsg;
        this.systemPrompt = systemPrompt;
        this.promptConfig = promptConfig != null ? promptConfig : Map.of();
        this.ttsVoiceId = ttsVoiceId;
        this.ttsVoiceJson = ttsVoiceJson;
        this.isActive = true;
    }

    public void update(String name, String modelId, String defaultAnim,
                       String greetingMsg, String systemPrompt, Map<String, Object> promptConfig,
                       String ttsVoiceId, Map<String, Object> ttsVoiceJson, Boolean isActive) {
        if (name != null) this.name = name;
        if (modelId != null) this.modelId = modelId;
        if (defaultAnim != null) this.defaultAnim = defaultAnim;
        if (greetingMsg != null) this.greetingMsg = greetingMsg;
        if (systemPrompt != null) this.systemPrompt = systemPrompt;
        if (promptConfig != null) this.promptConfig = promptConfig;
        if (ttsVoiceId != null) this.ttsVoiceId = ttsVoiceId;
        if (ttsVoiceJson != null) this.ttsVoiceJson = ttsVoiceJson;
        if (isActive != null) this.isActive = isActive;
    }
}
