package com.guideon.guideonbackend.domain.chat.service;

import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import com.guideon.core.domain.admin.entity.AdminRole;
import com.guideon.core.domain.admin.repository.AdminSiteRepository;
import com.guideon.guideonbackend.client.CoreChatClient;
import com.guideon.guideonbackend.domain.chat.dto.AdminChatStartRequest;
import com.guideon.guideonbackend.global.security.CustomAdminDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatTestService {

    private final CoreChatClient coreChatClient;
    private final AdminSiteRepository adminSiteRepository;

    public Map<String, String> startSession(AdminChatStartRequest request, CustomAdminDetails adminDetails) {
        validateSiteAccess(adminDetails, request.getSiteId());
        return coreChatClient.createSession(request.getDeviceId(), request.getSiteId());
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
