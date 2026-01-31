package com.guideon.guideonbackend.domain.site.service;

import com.guideon.guideonbackend.domain.site.dto.CreateSiteRequest;
import com.guideon.guideonbackend.domain.site.dto.SiteResponse;
import com.guideon.guideonbackend.domain.site.entity.Site;
import com.guideon.guideonbackend.domain.site.repository.SiteRepository;
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
}
