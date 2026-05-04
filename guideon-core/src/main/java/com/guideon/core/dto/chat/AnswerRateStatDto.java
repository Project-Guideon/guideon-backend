package com.guideon.core.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AnswerRateStatDto {
    private final long total;
    private final long found;
    private final long notFound;
    private final double rate;
}
