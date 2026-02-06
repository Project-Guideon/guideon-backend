package com.guideon.core.domain.site.repository;

import com.guideon.core.domain.site.entity.Site;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SiteRepository extends JpaRepository<Site, Long> {

    List<Site> findAllByOrderBySiteIdDesc();
}
