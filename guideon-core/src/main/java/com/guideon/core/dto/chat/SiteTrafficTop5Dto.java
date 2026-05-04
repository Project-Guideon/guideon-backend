package com.guideon.core.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SiteTrafficTop5Dto {
    private final List<SiteStat> sites;

    @Getter
    @AllArgsConstructor
    public static class SiteStat {
        private final Long siteId;
        private final String siteName;
        private final long count;
    }
}
