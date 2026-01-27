package com.guideon.guideonbackend.domain.site.repository;

import com.guideon.guideonbackend.domain.site.entity.Site;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteRepository extends JpaRepository<Site, Long> {
}
