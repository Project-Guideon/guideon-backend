package com.guideon.core.dto.admin;

import com.guideon.core.domain.admin.entity.Admin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorDto {

    private Long adminId;
    private String email;
    private String role;
    private Boolean isActive;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;

    public static OperatorDto from(Admin admin) {
        return OperatorDto.builder()
                .adminId(admin.getAdminId())
                .email(admin.getEmail())
                .role(admin.getRole().name())
                .isActive(admin.getIsActive())
                .lastLoginAt(admin.getLastLoginAt())
                .createdAt(admin.getCreatedAt())
                .build();
    }
}
