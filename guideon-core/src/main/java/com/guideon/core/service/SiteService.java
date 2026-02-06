package com.guideon.core.service;

import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import com.guideon.core.domain.site.entity.Site;
import com.guideon.core.domain.site.repository.SiteRepository;
import com.guideon.core.dto.CreateSiteCommand;
import com.guideon.core.dto.SiteDto;
import com.guideon.core.dto.UpdateSiteCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Core Site Service - 순수 비즈니스 로직
 * 인증/인가 로직 없음 (BFF에서 처리)
 */
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
    public SiteDto createSite(CreateSiteCommand command) {
        Site site = Site.builder()
                .name(command.getName())
                .build();

        siteRepository.save(site);
        log.info("관광지 생성 완료: siteId={}, name={}", site.getSiteId(), site.getName());

        return SiteDto.from(site);
    }

    /**
     * 관광지 목록 조회
     */
    public List<SiteDto> getAllSites() {
        return siteRepository.findAllByOrderBySiteIdDesc().stream()
                .map(SiteDto::from)
                .toList();
    }

    /**
     * 관광지 상세 조회
     */
    public SiteDto getSite(Long siteId) {
        Site site = findSiteById(siteId);
        return SiteDto.from(site);
    }

    /**
     * 관광지 수정
     */
    @Transactional
    public SiteDto updateSite(Long siteId, UpdateSiteCommand command) {
        Site site = findSiteById(siteId);
        site.updateName(command.getName());
        log.info("관광지 수정 완료: siteId={}, name={}", site.getSiteId(), site.getName());

        return SiteDto.from(site);
    }

    /**
     * 관광지 비활성화
     */
    @Transactional
    public void deactivateSite(Long siteId) {
        Site site = findSiteById(siteId);
        site.deactivate();
        log.info("관광지 비활성화 완료: siteId={}", siteId);
    }

    /**
     * 관광지 재활성화
     */
    @Transactional
    public void activateSite(Long siteId) {
        Site site = findSiteById(siteId);
        site.activate();
        log.info("관광지 재활성화 완료: siteId={}", siteId);
    }

    /**
     * 관광지 존재 여부 확인
     */
    public boolean existsById(Long siteId) {
        return siteRepository.existsById(siteId);
    }

    private Site findSiteById(Long siteId) {
        return siteRepository.findById(siteId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.NOT_FOUND,
                        "관광지를 찾을 수 없습니다: " + siteId
                ));
    }
}
