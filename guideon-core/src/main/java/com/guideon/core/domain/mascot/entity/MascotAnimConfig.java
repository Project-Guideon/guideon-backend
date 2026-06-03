package com.guideon.core.domain.mascot.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 사이트별 마스코트 애니메이션 설정.
 * state_key(idle/speaking 등) → clip_name(GLB 내 클립명) + glb_url(원본 GLB 파일 URL)
 * 1회 설정 후 마스코트 생성(rig 완료)마다 mesh-processor가 자동 병합에 사용.
 */
@Entity
@Table(name = "tb_mascot_anim_config")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MascotAnimConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "site_id", nullable = false)
    private Long siteId;

    /** 상태 키: idle | speaking | listening | thinking | greeting */
    @Column(name = "state_key", nullable = false, length = 20)
    private String stateKey;

    /** GLB 내부 클립명 — combiner.js가 이 이름으로 Animation을 임베드 */
    @Column(name = "clip_name", nullable = false, length = 50)
    private String clipName;

    /** 원본 GLB 파일 URL (admin-bff /internal/files/... 경로) */
    @Column(name = "glb_url", nullable = false, length = 500)
    private String glbUrl;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder
    public MascotAnimConfig(Long siteId, String stateKey, String clipName, String glbUrl) {
        this.siteId = siteId;
        this.stateKey = stateKey;
        this.clipName = clipName;
        this.glbUrl = glbUrl;
        this.updatedAt = Instant.now();
    }

    public void update(String clipName, String glbUrl) {
        this.clipName = clipName;
        this.glbUrl = glbUrl;
        this.updatedAt = Instant.now();
    }

    /** 클립명만 수정 (GLB 파일 변경 없이 매핑만 수정할 때) */
    public void updateClipName(String clipName) {
        this.clipName = clipName;
        this.updatedAt = Instant.now();
    }
}
