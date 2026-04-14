package com.guideon.core.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 질문 유형 통계 DTO
 *
 * 카테고리별 질문 수 + 비율
 * category 값: DIRECTION(길안내), INFORMATION(시설·역사 정보), OPERATION(운영·이벤트), SMALLTALK(일상대화), GENERAL(기타), ERROR(오류)
 */
@Getter
@AllArgsConstructor
public class QuestionTypeStatDto {

    private final List<CategoryStat> categories;
    private final long total;

    @Getter
    @AllArgsConstructor
    public static class CategoryStat {
        private final String category;
        private final long count;
        private final double ratio;
    }
}
