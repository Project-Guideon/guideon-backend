package com.guideon.guideonbackend.domain.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AdminMeResponse {

    @JsonProperty("admin_id")
    private Long adminId;

    private String email;

    private String role;

    @JsonProperty("site_ids")
    private List<Long> siteIds;
}
