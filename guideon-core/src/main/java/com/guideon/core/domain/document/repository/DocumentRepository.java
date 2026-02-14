package com.guideon.core.domain.document.repository;

import com.guideon.core.domain.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    boolean existsBySite_SiteIdAndFileHash(Long siteId, String fileHash);

    Optional<Document> findByDocIdAndSite_SiteId(Long docId, Long siteId);
}
