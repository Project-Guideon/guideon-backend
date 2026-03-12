package com.guideon.core.service;

import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import com.guideon.core.domain.dailyinfo.entity.DailyInfo;
import com.guideon.core.domain.dailyinfo.repository.DailyInfoRepository;
import com.guideon.core.domain.place.entity.Place;
import com.guideon.core.domain.place.repository.PlaceRepository;
import com.guideon.core.domain.site.repository.SiteRepository;
import com.guideon.core.dto.dailyinfo.DailyInfoDto;
import com.guideon.core.dto.dailyinfo.UpsertDailyInfoCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyInfoService {

    private final DailyInfoRepository dailyInfoRepository;
    private final PlaceRepository placeRepository;
    private final SiteRepository siteRepository;

    /**
     * 특정 날짜의 site 전체 일일 정보 조회 (Kiosk용)
     * date 미입력 시 오늘 날짜 기준
     */
    public List<DailyInfoDto> getDailyInfosBySiteAndDate(Long siteId, LocalDate date) {
        if (!siteRepository.existsById(siteId)) {
            throw new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 관광지입니다: " + siteId);
        }
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return dailyInfoRepository.findBySiteIdAndTargetDate(siteId, targetDate)
                .stream().map(DailyInfoDto::from).toList();
    }

    /**
     * 기간별 일일 정보 조회 (Admin용)
     */
    public List<DailyInfoDto> getDailyInfosByDateRange(Long siteId, LocalDate from, LocalDate to) {
        if (!siteRepository.existsById(siteId)) {
            throw new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 관광지입니다: " + siteId);
        }
        return dailyInfoRepository.findBySiteIdAndTargetDateBetween(siteId, from, to)
                .stream().map(DailyInfoDto::from).toList();
    }

    /**
     * 일일 정보 upsert (Admin용)
     * (placeId, targetDate, infoType) 기준으로 존재하면 update, 없으면 insert
     */
    @Transactional
    public DailyInfoDto upsert(Long siteId, LocalDate targetDate, UpsertDailyInfoCommand command) {
        Place place = placeRepository.findByPlaceIdAndSite_SiteId(command.getPlaceId(), siteId)
                .orElseThrow(() -> new CustomException(ErrorCode.PLACE_NOT_FOUND));

        Optional<DailyInfo> existing = dailyInfoRepository
                .findByPlace_PlaceIdAndTargetDateAndInfoType(
                        command.getPlaceId(), targetDate, command.getInfoType());

        DailyInfo dailyInfo;
        if (existing.isPresent()) {
            dailyInfo = existing.get();
            dailyInfo.updateContent(command.getContent());
        } else {
            dailyInfo = DailyInfo.builder()
                    .siteId(siteId)
                    .place(place)
                    .targetDate(targetDate)
                    .infoType(command.getInfoType())
                    .content(command.getContent())
                    .build();
            dailyInfoRepository.save(dailyInfo);
        }

        log.info("DailyInfo upsert: siteId={}, placeId={}, date={}, type={}",
                siteId, command.getPlaceId(), targetDate, command.getInfoType());
        return DailyInfoDto.from(dailyInfo);
    }

    /**
     * 일일 정보 삭제 (Admin용)
     */
    @Transactional
    public void delete(Long siteId, Long dailyInfoId) {
        DailyInfo dailyInfo = dailyInfoRepository.findById(dailyInfoId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "일일 정보를 찾을 수 없습니다"));

        if (!dailyInfo.getSiteId().equals(siteId)) {
            throw new CustomException(ErrorCode.ADMIN_SITE_FORBIDDEN);
        }

        dailyInfoRepository.delete(dailyInfo);
    }
}