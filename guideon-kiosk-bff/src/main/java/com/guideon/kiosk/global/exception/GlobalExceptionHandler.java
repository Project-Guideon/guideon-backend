package com.guideon.kiosk.global.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import com.guideon.common.response.ApiError;
import com.guideon.common.response.ApiResponse;
import com.guideon.kiosk.global.trace.TraceIdUtil;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private String traceId(HttpServletRequest req) {
        Object v = req.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return (v == null) ? null : v.toString();
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(
            CustomException e,
            HttpServletRequest req
    ) {
        log.warn("[{}] CustomException: {} - {}", traceId(req), e.getErrorCode().name(), e.getMessage());

        ApiError apiError = ApiError.builder()
                .code(e.getErrorCode().name())
                .message(e.getMessage())
                .details(e.getDetails())
                .build();

        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ApiResponse.fail(apiError, traceId(req)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException e,
            HttpServletRequest req
    ) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        log.warn("[{}] Validation failed: {} field(s) invalid", traceId(req), fieldErrors.size());

        List<Map<String, String>> errors = fieldErrors.stream()
                .map(fe -> Map.of(
                        "field", fe.getField(),
                        "reason", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value"
                ))
                .toList();

        Map<String, Object> details = new HashMap<>();
        details.put("fields", errors);

        ApiError apiError = ApiError.builder()
                .code(ErrorCode.VALIDATION_ERROR.name())
                .message(ErrorCode.VALIDATION_ERROR.getMessage())
                .details(details)
                .build();

        return ResponseEntity
                .status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
                .body(ApiResponse.fail(apiError, traceId(req)));
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiResponse<Void>> handleFeignException(
            FeignException e,
            HttpServletRequest req
    ) {
        log.warn("[{}] FeignException: status={}, message={}", traceId(req), e.status(), e.getMessage());

        String body = e.contentUTF8();
        if (body != null && !body.isBlank()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(body);
                JsonNode errorNode = root.get("error");
                if (errorNode != null && !errorNode.isNull()) {
                    ApiError coreError = mapper.treeToValue(errorNode, ApiError.class);
                    return ResponseEntity
                            .status(e.status())
                            .body(ApiResponse.fail(coreError, traceId(req)));
                }
            } catch (Exception parseEx) {
                log.warn("[{}] Core 에러 응답 파싱 실패: {}", traceId(req), parseEx.getMessage());
            }
        }

        HttpStatus status = HttpStatus.resolve(e.status());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;

        ApiError apiError = ApiError.builder()
                .code(status.is4xxClientError() ? "CORE_CLIENT_ERROR" : "CORE_SERVER_ERROR")
                .message("Core 서비스 호출 중 오류가 발생했습니다")
                .build();

        return ResponseEntity
                .status(status)
                .body(ApiResponse.fail(apiError, traceId(req)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAny(
            Exception e,
            HttpServletRequest req
    ) {
        log.error("[{}] Unhandled exception: {}", traceId(req), e.getMessage(), e);

        ApiError apiError = ApiError.builder()
                .code(ErrorCode.INTERNAL_ERROR.name())
                .message(ErrorCode.INTERNAL_ERROR.getMessage())
                .details(null)
                .build();

        return ResponseEntity
                .status(ErrorCode.INTERNAL_ERROR.getHttpStatus())
                .body(ApiResponse.fail(apiError, traceId(req)));
    }
}