package com.guideon.core.dto.chat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class SiteTrafficTop5Dto {
    private final List<SiteStat> sites;

    @JsonCreator
    public SiteTrafficTop5Dto(@JsonProperty("sites") List<SiteStat> sites) {
        this.sites = sites;
    }

    @Getter
    public static class SiteStat {
        private final Long siteId;
        private final String siteName;
        private final long count;

        @JsonCreator
        public SiteStat(@JsonProperty("siteId") Long siteId,
                        @JsonProperty("siteName") String siteName,
                        @JsonProperty("count") long count) {
            this.siteId = siteId;
            this.siteName = siteName;
            this.count = count;
        }
    }
}
