package com.guideon.core.domain.mascot.repository;

import com.guideon.core.domain.mascot.entity.MascotAnimConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MascotAnimConfigRepository extends JpaRepository<MascotAnimConfig, Long> {

    List<MascotAnimConfig> findBySiteId(Long siteId);

    Optional<MascotAnimConfig> findBySiteIdAndStateKey(Long siteId, String stateKey);

    boolean existsBySiteId(Long siteId);
}
