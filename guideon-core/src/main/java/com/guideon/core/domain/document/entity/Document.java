package com.guideon.core.domain.document.entity;

import com.guideon.core.domain.site.entity.Site;
import com.guideon.core.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tb_document",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_doc_hash", columnNames = {"site_id", "file_hash"}),
                @UniqueConstraint(name = "uk_doc_id_site", columnNames = {"doc_id", "site_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Document extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "doc_id")
    private Long docId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "storage_url", nullable = false, length = 500)
    private String storageUrl;

    @Column(name = "file_hash", nullable = false, length = 64)
    private String fileHash;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "chunk_size", nullable = false)
    private Integer chunkSize;

    @Column(name = "chunk_overlap", nullable = false)
    private Integer chunkOverlap;

    @Column(name = "embedding_model", nullable = false, length = 100)
    private String embeddingModel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "doc_status_enum")
    private DocStatus status;

    @Column(name = "failed_reason", columnDefinition = "TEXT")
    private String failedReason;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Builder
    public Document(Site site, String originalName, String storageUrl,
                    String fileHash, Long fileSize, Integer chunkSize,
                    Integer chunkOverlap, String embeddingModel) {
        this.site = site;
        this.originalName = originalName;
        this.storageUrl = storageUrl;
        this.fileHash = fileHash;
        this.fileSize = fileSize;
        this.chunkSize = chunkSize != null ? chunkSize : 500;
        this.chunkOverlap = chunkOverlap != null ? chunkOverlap : 50;
        this.embeddingModel = embeddingModel != null ? embeddingModel : "text-embedding-3-small";
        this.status = DocStatus.PENDING;
    }
}
