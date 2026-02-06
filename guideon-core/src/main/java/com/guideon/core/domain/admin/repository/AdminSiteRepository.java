package com.guideon.core.domain.admin.repository;

import com.guideon.core.domain.admin.entity.AdminSite;
import com.guideon.core.domain.admin.entity.AdminSiteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdminSiteRepository extends JpaRepository<AdminSite, AdminSiteId> {

    @Query("SELECT a.id.siteId FROM AdminSite a WHERE a.id.adminId = :adminId")
    List<Long> findSiteIdsByAdminId(@Param("adminId") Long adminId);

    boolean existsById_AdminIdAndId_SiteId(Long adminId, Long siteId);
}
