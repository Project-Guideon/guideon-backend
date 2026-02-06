package com.guideon.guideonbackend.global.exception;

import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import com.guideon.common.response.ApiError;
import com.guideon.common.response.ApiResponse;
import com.guideon.guideonbackend.global.trace.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * Request에서 trace_id 추출
     * TraceIdFilter가 request.setAttribute()로 저장해둔 값을 조회
     * 모든 에러 응답에 동일한 trace_id를 포함
     */
    private String traceId(HttpServletRequest req) {
        Object v = req.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        return (v == null) ? null : v.toString();
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(
            CustomException e,
            HttpServletRequest req
    ) {
        log.warn("[{}] CustomException: {} - {}",
                traceId(req),
                e.getErrorCode().name(),
                e.getMessage());

        ApiError apiError = ApiError.builder()
                .code(e.getErrorCode().name())
                .message(e.getMessage())
                .details(e.getDetails())  // CustomException에서 제공한 추가 정보
                .build();

        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ApiResponse.fail(apiError, traceId(req)));
    }

    //Validation 예외 처리 (400 Bad Request)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException e,
            HttpServletRequest req
    ) {

        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();

        log.warn("[{}] Validation failed: {} field(s) invalid",
                traceId(req),
                fieldErrors.size());

        //모든 필드 에러를 리스트로 변환
        List<Map<String, String>> errors = fieldErrors.stream()
                .map(fe -> Map.of(
                        "field", fe.getField(),
                        "reason", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value"
                ))
                .toList();

        //details에 모든 에러 포함
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

    /**
     * 예상치 못한 모든 예외 처리 (500 Internal Server Error)
     * 위에서 처리하지 못한 모든 예외의 Fallback 핸들러
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAny(
            Exception e,
            HttpServletRequest req
    ) {
        // 스택 트레이스 전체를 로그에 기록하여 디버깅 가능하도록 함
        log.error("[{}] Unhandled exception: {}",
                traceId(req),
                e.getMessage(),
                e);  // 세 번째 인자로 예외를 넘기면 스택 트레이스 자동 출력

        ApiError apiError = ApiError.builder()
                .code(ErrorCode.INTERNAL_ERROR.name())
                .message(ErrorCode.INTERNAL_ERROR.getMessage())
                .details(null)  // 내부 에러는 details를 비움 (민감 정보 노출 방지)
                .build();

        return ResponseEntity
                .status(ErrorCode.INTERNAL_ERROR.getHttpStatus())
                .body(ApiResponse.fail(apiError, traceId(req)));
    }
}
