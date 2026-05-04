package com.guideon.core.dto.chat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class HourlyTrafficStatDto {
    private final List<HourStat> hours;

    @JsonCreator
    public HourlyTrafficStatDto(@JsonProperty("hours") List<HourStat> hours) {
        this.hours = hours;
    }

    @Getter
    public static class HourStat {
        private final int hour;
        private final long count;

        @JsonCreator
        public HourStat(@JsonProperty("hour") int hour,
                        @JsonProperty("count") long count) {
            this.hour = hour;
            this.count = count;
        }
    }
}
