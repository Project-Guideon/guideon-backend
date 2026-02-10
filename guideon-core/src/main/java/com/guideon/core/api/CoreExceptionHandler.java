package com.guideon.core.api;

import com.guideon.common.exception.CustomException;
import com.guideon.common.response.ApiError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class CoreExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiError> handleCustomException(CustomException e) {
        log.warn("CustomException: {} - {}", e.getErrorCode().name(), e.getMessage());

        ApiError apiError = ApiError.builder()
                .code(e.getErrorCode().name())
                .message(e.getMessage())
                .details(e.getDetails())
                .build();

        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(apiError);
    }
}
