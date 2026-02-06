package com.guideon.guideonbackend.domain.admin.repository;

import com.guideon.guideonbackend.domain.admin.entity.AdminInvite;
import com.guideon.guideonbackend.domain.admin.entity.AdminInviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminInviteRepository extends JpaRepository<AdminInvite, Long> {

    Optional<AdminInvite> findByTokenHash(String tokenHash);

    boolean existsBySite_SiteIdAndEmailAndStatus(Long siteId, String email, AdminInviteStatus status);

    List<AdminInvite> findAllByOrderByCreatedAtDesc();
}
