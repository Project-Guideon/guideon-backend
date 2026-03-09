package com.guideon.core.domain.mascot.repository;

import com.guideon.core.domain.mascot.entity.Mascot;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MascotRepository extends JpaRepository<Mascot, Long> {

    @EntityGraph(attributePaths = "site")
    Optional<Mascot> findBySite_SiteId(Long siteId);

    boolean existsBySite_SiteId(Long siteId);
}
