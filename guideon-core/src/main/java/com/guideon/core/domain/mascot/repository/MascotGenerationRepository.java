package com.guideon.core.domain.mascot.repository;

import com.guideon.core.domain.mascot.entity.MascotGeneration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MascotGenerationRepository extends JpaRepository<MascotGeneration, Long> {

    Optional<MascotGeneration> findTopBySite_SiteIdOrderByCreatedAtDesc(Long siteId);

    List<MascotGeneration> findBySite_SiteIdOrderByCreatedAtDesc(Long siteId);
}
