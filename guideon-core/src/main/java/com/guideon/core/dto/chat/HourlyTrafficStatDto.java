package com.guideon.core.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class HourlyTrafficStatDto {
    private final List<HourStat> hours;

    @Getter
    @AllArgsConstructor
    public static class HourStat {
        private final int hour;
        private final long count;
    }
}
