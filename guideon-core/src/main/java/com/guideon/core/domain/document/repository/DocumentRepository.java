package com.guideon.core.domain.document.repository;

import com.guideon.core.domain.document.entity.Document;
import com.guideon.core.domain.document.entity.DocStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    boolean existsBySite_SiteIdAndFileHash(Long siteId, String fileHash);

    List<Document> findByStatusAndCreatedAtBefore(DocStatus status, LocalDateTime before);

    Optional<Document> findByDocIdAndSite_SiteId(Long docId, Long siteId);

    @Query(value = """
            SELECT d.* FROM tb_document d
            WHERE d.site_id = :siteId
            AND (CAST(:keyword AS TEXT) IS NULL OR LOWER(d.original_name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            AND (CAST(:status AS TEXT) IS NULL OR d.status = :status)
            """,
            countQuery = """
            SELECT COUNT(*) FROM tb_document d
            WHERE d.site_id = :siteId
            AND (CAST(:keyword AS TEXT) IS NULL OR LOWER(d.original_name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            AND (CAST(:status AS TEXT) IS NULL OR d.status = :status)
            """,
            nativeQuery = true)
    Page<Document> findByFilters(
            @Param("siteId") Long siteId,
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable);
}
