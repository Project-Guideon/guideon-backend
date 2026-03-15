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

    @Column(name = "model_status", nullable = false, length = 20)
    private String modelStatus;

    @Column(name = "rig_task_id", length = 100)
    private String rigTaskId;

    @Column(name = "rig_status", nullable = false, length = 20)
    private String rigStatus;

    @Column(name = "result_model_url", length = 500)
    private String resultModelUrl;

    @Column(name = "failed_reason", columnDefinition = "TEXT")
    private String failedReason;

    @Builder
    public MascotGeneration(Site site, String sourceImageUrl) {
        this.site = site;
        this.sourceImageUrl = sourceImageUrl;
        this.modelStatus = "PENDING";
        this.rigStatus = "PENDING";
    }

    public void startModelGeneration(String modelTaskId) {
        this.modelTaskId = modelTaskId;
        this.modelStatus = "PROCESSING";
    }

    public void completeModelGeneration() {
        this.modelStatus = "SUCCESS";
    }

    public void failModelGeneration(String reason) {
        this.modelStatus = "FAILED";
        this.failedReason = reason;
    }

    public void startRigging(String rigTaskId) {
        this.rigTaskId = rigTaskId;
        this.rigStatus = "PROCESSING";
    }

    public void completeRigging(String resultModelUrl) {
        this.rigStatus = "SUCCESS";
        this.resultModelUrl = resultModelUrl;
    }

    public void failRigging(String reason) {
        this.rigStatus = "FAILED";
        this.failedReason = reason;
    }

    public boolean isFullyCompleted() {
        return "SUCCESS".equals(modelStatus) && "SUCCESS".equals(rigStatus);
    }

    public boolean isFailed() {
        return "FAILED".equals(modelStatus) || "FAILED".equals(rigStatus);
    }
}
