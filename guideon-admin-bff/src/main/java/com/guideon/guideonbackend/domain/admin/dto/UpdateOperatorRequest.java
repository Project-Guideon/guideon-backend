package com.guideon.guideonbackend.domain.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateOperatorRequest {

    @JsonProperty("is_active")
    private Boolean isActive;
}
