package com.guideon.guideonbackend.domain.site.service;

import com.guideon.core.dto.CreateSiteCommand;
import com.guideon.core.dto.SiteDto;
import com.guideon.core.dto.UpdateSiteCommand;
import com.guideon.guideonbackend.client.CoreSiteClient;
import com.guideon.guideonbackend.domain.site.dto.CreateSiteRequest;
import com.guideon.guideonbackend.domain.site.dto.SiteResponse;
import com.guideon.guideonbackend.domain.site.dto.UpdateSiteRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Admin BFF Site Service
 * Core Service를 Feign Client로 호출하여 Site 관련 기능 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SiteService {

    private final CoreSiteClient coreSiteClient;

    /**
     * 관광지 생성
     */
    public SiteResponse createSite(CreateSiteRequest request) {
        CreateSiteCommand command = CreateSiteCommand.builder()
                .name(request.getName())
                .build();

        SiteDto siteDto = coreSiteClient.createSite(command);
        log.info("관광지 생성 완료: siteId={}, name={}", siteDto.getSiteId(), siteDto.getName());

        return SiteResponse.from(siteDto);
    }

    /**
     * 관광지 목록 조회
     */
    public List<SiteResponse> getAllSites() {
        return coreSiteClient.getAllSites().stream()
                .map(SiteResponse::from)
                .toList();
    }

    /**
     * 관광지 상세 조회
     */
    public SiteResponse getSite(Long siteId) {
        SiteDto siteDto = coreSiteClient.getSite(siteId);
        return SiteResponse.from(siteDto);
    }

    /**
     * 관광지 수정
     */
    public SiteResponse updateSite(Long siteId, UpdateSiteRequest request) {
        UpdateSiteCommand command = UpdateSiteCommand.builder()
                .name(request.getName())
                .build();

        SiteDto siteDto = coreSiteClient.updateSite(siteId, command);
        log.info("관광지 수정 완료: siteId={}, name={}", siteDto.getSiteId(), siteDto.getName());

        return SiteResponse.from(siteDto);
    }

    /**
     * 관광지 비활성화
     */
    public void deactivateSite(Long siteId) {
        coreSiteClient.deactivateSite(siteId);
        log.info("관광지 비활성화 완료: siteId={}", siteId);
    }

    /**
     * 관광지 재활성화
     */
    public void activateSite(Long siteId) {
        coreSiteClient.activateSite(siteId);
        log.info("관광지 재활성화 완료: siteId={}", siteId);
    }
}
