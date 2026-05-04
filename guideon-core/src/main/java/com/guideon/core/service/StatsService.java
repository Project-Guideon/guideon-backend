package com.guideon.core.service;

import com.guideon.core.domain.chat.repository.ChatMessageRepository;
import com.guideon.core.domain.device.repository.DeviceRepository;
import com.guideon.core.dto.chat.AnswerRateStatDto;
import com.guideon.core.dto.chat.HourlyTrafficStatDto;
import com.guideon.core.dto.chat.QuestionTypeStatDto;
import com.guideon.core.dto.chat.SiteTrafficTop5Dto;
import com.guideon.core.dto.device.DeviceStatusStatDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    private final ChatMessageRepository chatMessageRepository;
    private final DeviceRepository deviceRepository;

    /**
     * 사이트별 질문 유형 통계
     * category 컬럼 GROUP BY → 비율 계산
     */
    public QuestionTypeStatDto getQuestionTypeStats(Long siteId) {
        List<ChatMessageRepository.CategoryCountProjection> rows =
                chatMessageRepository.countByCategoryForSite(siteId);

        long total = rows.stream().mapToLong(ChatMessageRepository.CategoryCountProjection::getCount).sum();

        List<QuestionTypeStatDto.CategoryStat> categories = rows.stream()
                .map(row -> new QuestionTypeStatDto.CategoryStat(
                        row.getCategory() != null ? row.getCategory() : "GENERAL",
                        row.getCount(),
                        total > 0 ? Math.round((double) row.getCount() / total * 1000) / 1000.0 : 0.0
                ))
                .toList();

        return new QuestionTypeStatDto(categories, total);
    }

    /**
     * 사이트별 AI 답변 성공률
     * answer_found=true 비율 계산
     */
    public AnswerRateStatDto getAnswerRateStat(Long siteId) {
        ChatMessageRepository.AnswerRateProjection row =
                chatMessageRepository.countAnswerRateForSite(siteId);

        long total = row.getTotal() != null ? row.getTotal() : 0L;
        long found = row.getFound() != null ? row.getFound() : 0L;
        long notFound = total - found;
        double rate = total > 0 ? Math.round((double) found / total * 1000) / 1000.0 : 0.0;

        return new AnswerRateStatDto(total, found, notFound, rate);
    }

    /**
     * 사이트별 시간대별 요청량 (오늘 기준, 0~23시)
     */
    public HourlyTrafficStatDto getHourlyTrafficStat(Long siteId) {
        List<ChatMessageRepository.HourCountProjection> rows =
                chatMessageRepository.countByHourForSite(siteId);

        List<HourlyTrafficStatDto.HourStat> hours = rows.stream()
                .map(r -> new HourlyTrafficStatDto.HourStat(r.getHour(), r.getMsgCount()))
                .toList();

        return new HourlyTrafficStatDto(hours);
    }

    /**
     * 전체 관광지 트래픽 Top 5 (PLATFORM_ADMIN 전용)
     */
    public SiteTrafficTop5Dto getSiteTrafficTop5() {
        List<ChatMessageRepository.SiteTrafficProjection> rows =
                chatMessageRepository.countTop5BySite();

        List<SiteTrafficTop5Dto.SiteStat> sites = rows.stream()
                .map(r -> new SiteTrafficTop5Dto.SiteStat(r.getSiteId(), r.getSiteName(), r.getMsgCount()))
                .toList();

        return new SiteTrafficTop5Dto(sites);
    }

    /**
     * 사이트별 기기 상태 통계
     * 정상: isActive=true + lastPing 30분 이내
     * 점검: isActive=false
     * 장애: isActive=true + lastPing 없거나 30분 초과
     */
    public DeviceStatusStatDto getDeviceStatusStat(Long siteId) {
        DeviceRepository.DeviceStatusProjection row =
                deviceRepository.countDeviceStatusBySite(siteId);

        long total = row.getTotal() != null ? row.getTotal() : 0L;
        long normal = row.getNormal() != null ? row.getNormal() : 0L;
        long maintenance = row.getMaintenance() != null ? row.getMaintenance() : 0L;
        long failure = row.getFailure() != null ? row.getFailure() : 0L;

        return new DeviceStatusStatDto(total, normal, maintenance, failure);
    }
}
