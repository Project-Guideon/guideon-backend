package com.guideon.core.domain.mascot.entity;

import com.guideon.core.domain.site.entity.Site;
import com.guideon.core.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_mascot_generation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MascotGeneration extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "generation_id")
    private Long generationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "source_image_url", nullable = false, length = 500)
    private String sourceImageUrl;

    @Column(name = "model_task_id", length = 100)
    private String modelTaskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "model_status", nullable = false, length = 20)
    private GenerationStatus modelStatus;

    @Column(name = "rig_task_id", length = 100)
    private String rigTaskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rig_status", nullable = false, length = 20)
    private GenerationStatus rigStatus;

    // animate_retarget: animations 배열 1개 task → GLB 1개에 5클립
    @Enumerated(EnumType.STRING)
    @Column(name = "retarget_status", nullable = false, length = 20)
    private GenerationStatus retargetStatus;

    @Column(name = "retarget_task_id", length = 100)
    private String retargetTaskId;

    @Column(name = "anim_model_url", length = 500)
    private String animModelUrl;  // retarget 완료 GLB URL

    @Column(name = "result_model_url", length = 500)
    private String resultModelUrl;

    @Column(name = "failed_reason", columnDefinition = "TEXT")
    private String failedReason;

    @Builder
    public MascotGeneration(Site site, String sourceImageUrl) {
        this.site = site;
        this.sourceImageUrl = sourceImageUrl;
        this.modelStatus = GenerationStatus.PENDING;
        this.rigStatus = GenerationStatus.PENDING;
        this.retargetStatus = GenerationStatus.PENDING;
    }

    // ── model 단계 ──

    public void startModelGeneration(String modelTaskId) {
        this.modelTaskId = modelTaskId;
        this.modelStatus = GenerationStatus.PROCESSING;
    }

    public void completeModelGeneration() {
        this.modelStatus = GenerationStatus.SUCCESS;
    }

    public void failModelGeneration(String reason) {
        this.modelStatus = GenerationStatus.FAILED;
        this.failedReason = appendReason(this.failedReason, reason);
    }

    // ── rig 단계 ──

    public void startRigging(String rigTaskId) {
        this.rigTaskId = rigTaskId;
        this.rigStatus = GenerationStatus.PROCESSING;
    }

    public void completeRigging(String resultModelUrl) {
        this.rigStatus = GenerationStatus.SUCCESS;
        this.resultModelUrl = resultModelUrl;
    }

    public void failRigging(String reason) {
        this.rigStatus = GenerationStatus.FAILED;
        this.failedReason = appendReason(this.failedReason, reason);
    }

    // ── retarget 단계 (animations 배열 1개 task) ──

    public void startRetarget(String retargetTaskId) {
        this.retargetTaskId = retargetTaskId;
        this.retargetStatus = GenerationStatus.PROCESSING;
    }

    public void completeRetarget(String animModelUrl) {
        this.retargetStatus = GenerationStatus.SUCCESS;
        this.animModelUrl = animModelUrl;
    }

    /** retarget 실패는 치명적이지 않음 — base GLB는 이미 확보됨. */
    public void failRetarget(String reason) {
        this.retargetStatus = GenerationStatus.FAILED;
        this.failedReason = appendReason(this.failedReason, "retarget: " + reason);
    }

    // ── 완료/실패 판단 ──

    /**
     * retarget은 SUCCESS/FAILED 어느 쪽이든 종료되면 완료로 간주.
     * (base GLB는 rig 완료 시점에 이미 확보됨)
     */
    public boolean isFullyCompleted() {
        return modelStatus == GenerationStatus.SUCCESS
                && rigStatus == GenerationStatus.SUCCESS
                && (retargetStatus == GenerationStatus.SUCCESS
                        || retargetStatus == GenerationStatus.FAILED);
    }

    /** model/rig 실패만 전체 실패 — retarget 실패는 폴백 허용. */
    public boolean isFailed() {
        return modelStatus == GenerationStatus.FAILED || rigStatus == GenerationStatus.FAILED;
    }

    // ── 상태값 문자열 반환 (하위 호환) ──

    public String getModelStatusValue() { return modelStatus.name(); }
    public String getRigStatusValue()   { return rigStatus.name(); }
    public String getRetargetStatusValue() { return retargetStatus.name(); }

    private String appendReason(String existing, String newReason) {
        if (existing == null || existing.isBlank()) return newReason;
        return existing + " | " + newReason;
    }
}
