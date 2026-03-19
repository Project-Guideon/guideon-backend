package com.guideon.guideonbackend.domain.admin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.guideon.core.dto.admin.OperatorDto;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OperatorResponse {

    @JsonProperty("admin_id")
    private Long adminId;

    private String email;

    private String role;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("last_login_at")
    private LocalDateTime lastLoginAt;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    public static OperatorResponse from(OperatorDto dto) {
        return OperatorResponse.builder()
                .adminId(dto.getAdminId())
                .email(dto.getEmail())
                .role(dto.getRole())
                .isActive(dto.getIsActive())
                .lastLoginAt(dto.getLastLoginAt())
                .createdAt(dto.getCreatedAt())
                .build();
    }
}
