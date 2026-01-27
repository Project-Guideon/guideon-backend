package com.guideon.guideonbackend.global.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private ApiError error;

    @JsonProperty("trace_id")
    private String traceId;

    public static <T> ApiResponse<T> success(T data, String traceId) {
        //성공 응답
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .error(null)
                .traceId(traceId)
                .build();
    }

    public static <T> ApiResponse<T> fail(ApiError error, String traceId) {
        //실패 응답
        return ApiResponse.<T>builder()
                .success(false)
                .data(null)
                .error(error)
                .traceId(traceId)
                .build();
    }
}
