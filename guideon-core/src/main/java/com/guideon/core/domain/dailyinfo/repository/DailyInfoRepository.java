package com.guideon.core.domain.dailyinfo.repository;

import com.guideon.core.domain.dailyinfo.entity.DailyInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyInfoRepository extends JpaRepository<DailyInfo, Long> {

    List<DailyInfo> findBySiteIdAndTargetDate(Long siteId, LocalDate targetDate);

    List<DailyInfo> findBySiteIdAndTargetDateBetween(Long siteId, LocalDate from, LocalDate to);

    Optional<DailyInfo> findByPlace_PlaceIdAndTargetDateAndInfoType(
            Long placeId, LocalDate targetDate, String infoType);
}