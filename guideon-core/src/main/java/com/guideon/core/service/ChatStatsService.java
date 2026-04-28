package com.guideon.core.service;

import com.guideon.core.domain.chat.repository.ChatMessageRepository;
import com.guideon.core.dto.chat.QuestionTypeStatDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatStatsService {

    private final ChatMessageRepository chatMessageRepository;

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
}
