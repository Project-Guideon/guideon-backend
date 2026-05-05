package com.guideon.guideonbackend.domain.stats.service;

import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import com.guideon.core.domain.admin.entity.AdminRole;
import com.guideon.core.domain.admin.repository.AdminSiteRepository;
import com.guideon.core.dto.chat.AnswerRateStatDto;
import com.guideon.core.dto.chat.HourlyTrafficStatDto;
import com.guideon.core.dto.chat.QuestionTypeStatDto;
import com.guideon.core.dto.chat.SiteTrafficTop5Dto;
import com.guideon.core.dto.device.DeviceStatusStatDto;
import com.guideon.guideonbackend.client.CoreStatsClient;
import com.guideon.guideonbackend.global.security.CustomAdminDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final CoreStatsClient coreStatsClient;
    private final AdminSiteRepository adminSiteRepository;

    public QuestionTypeStatDto getQuestionTypeStats(Long siteId, CustomAdminDetails adminDetails) {
        validateSiteAccess(adminDetails, siteId);
        return coreStatsClient.getQuestionTypeStats(siteId);
    }

    public AnswerRateStatDto getAnswerRateStat(Long siteId, CustomAdminDetails adminDetails) {
        validateSiteAccess(adminDetails, siteId);
        return coreStatsClient.getAnswerRateStat(siteId);
    }

    public HourlyTrafficStatDto getHourlyTrafficStat(Long siteId, CustomAdminDetails adminDetails) {
        validateSiteAccess(adminDetails, siteId);
        return coreStatsClient.getHourlyTrafficStat(siteId);
    }

    public SiteTrafficTop5Dto getSiteTrafficTop5() {
        return coreStatsClient.getSiteTrafficTop5();
    }

    public DeviceStatusStatDto getDeviceStatusStat(Long siteId, CustomAdminDetails adminDetails) {
        validateSiteAccess(adminDetails, siteId);
        return coreStatsClient.getDeviceStatusStat(siteId);
    }

    private void validateSiteAccess(CustomAdminDetails adminDetails, Long siteId) {
        if (adminDetails == null || adminDetails.getRole() == null) {
            throw new CustomException(ErrorCode.ADMIN_SITE_FORBIDDEN);
        }
        if (AdminRole.PLATFORM_ADMIN.name().equals(adminDetails.getRole())) {
            return;
        }
        if (AdminRole.SITE_ADMIN.name().equals(adminDetails.getRole())) {
            if (!adminSiteRepository.existsById_AdminIdAndId_SiteId(adminDetails.getAdminId(), siteId)) {
                throw new CustomException(ErrorCode.ADMIN_SITE_FORBIDDEN);
            }
            return;
        }
        throw new CustomException(ErrorCode.ADMIN_SITE_FORBIDDEN);
    }
}
