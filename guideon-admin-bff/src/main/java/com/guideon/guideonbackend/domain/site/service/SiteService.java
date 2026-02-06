package com.guideon.guideonbackend.domain.site.service;

import com.guideon.guideonbackend.domain.site.dto.CreateSiteRequest;
import com.guideon.guideonbackend.domain.site.dto.SiteResponse;
import com.guideon.guideonbackend.domain.site.dto.UpdateSiteRequest;
import com.guideon.core.domain.site.entity.Site;
import com.guideon.core.domain.site.repository.SiteRepository;
import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiteService {

    private final SiteRepository siteRepository;

    /**
     * 관광지 생성
     */
    @Transactional
    public SiteResponse createSite(CreateSiteRequest request) {
        Site site = Site.builder()
                .name(request.getName())
                .build();

        siteRepository.save(site);
        log.info("관광지 생성 완료: siteId={}, name={}", site.getSiteId(), site.getName());

        return SiteResponse.from(site);
    }

    /**
     * 관광지 목록 조회
     */
    public List<SiteResponse> getAllSites() {
        return siteRepository.findAllByOrderBySiteIdDesc().stream()
                .map(SiteResponse::from)
                .toList();
    }

    /**
     * 관광지 상세 조회
     */
    public SiteResponse getSite(Long siteId) {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.NOT_FOUND,
                        "관광지를 찾을 수 없습니다"
                ));
        return SiteResponse.from(site);
    }

    /**
     * 관광지 수정
     */
    @Transactional
    public SiteResponse updateSite(Long siteId, UpdateSiteRequest request) {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.NOT_FOUND,
                        "관광지를 찾을 수 없습니다"
                ));

        site.updateName(request.getName());
        log.info("관광지 수정 완료: siteId={}, name={}", site.getSiteId(), site.getName());

        return SiteResponse.from(site);
    }

    /**
     * 관광지 비활성화
     */
    @Transactional
    public void deactivateSite(Long siteId) {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.NOT_FOUND,
                        "관광지를 찾을 수 없습니다"
                ));

        site.deactivate();
        log.info("관광지 비활성화 완료: siteId={}", siteId);
    }

    /**
     * 관광지 재활성화
     */
    @Transactional
    public void activateSite(Long siteId) {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.NOT_FOUND,
                        "관광지를 찾을 수 없습니다"
                ));

        site.activate();
        log.info("관광지 재활성화 완료: siteId={}", siteId);
    }
}
