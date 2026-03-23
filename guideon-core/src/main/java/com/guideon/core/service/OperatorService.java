package com.guideon.core.service;

import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import com.guideon.core.domain.admin.entity.Admin;
import com.guideon.core.domain.admin.repository.AdminRepository;
import com.guideon.core.domain.admin.repository.AdminSiteRepository;
import com.guideon.core.domain.site.repository.SiteRepository;
import com.guideon.core.dto.admin.OperatorDto;
import com.guideon.core.dto.admin.UpdateOperatorCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OperatorService {

    private final AdminRepository adminRepository;
    private final AdminSiteRepository adminSiteRepository;
    private final SiteRepository siteRepository;

    public List<OperatorDto> getSiteOperators(Long siteId) {
        validateSiteExists(siteId);

        List<Admin> admins = adminSiteRepository.findAdminsBySiteId(siteId);
        return admins.stream().map(OperatorDto::from).toList();
    }

    @Transactional
    public OperatorDto updateOperatorStatus(Long siteId, Long operatorId, UpdateOperatorCommand command) {
        if (command.getIsActive() == null) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "isActive 값은 필수입니다");
        }

        validateSiteExists(siteId);

        if (!adminSiteRepository.existsById_AdminIdAndId_SiteId(operatorId, siteId)) {
            throw new CustomException(ErrorCode.NOT_FOUND,
                    "해당 사이트에 소속된 운영자가 아닙니다: operatorId=" + operatorId);
        }

        Admin admin = adminRepository.findById(operatorId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND,
                        "존재하지 않는 운영자입니다: " + operatorId));

        if (command.getIsActive()) {
            admin.activate();
        } else {
            admin.deactivate();
        }

        log.info("운영자 상태 변경: operatorId={}, siteId={}, isActive={}", operatorId, siteId, admin.getIsActive());
        return OperatorDto.from(admin);
    }

    private void validateSiteExists(Long siteId) {
        if (!siteRepository.existsById(siteId)) {
            throw new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 관광지입니다: " + siteId);
        }
    }
}
