package com.guideon.kiosk.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guideon.common.exception.ErrorCode;
import com.guideon.common.response.ApiError;
import com.guideon.common.response.ApiResponse;
import com.guideon.kiosk.global.trace.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Device 인증 실패 시 401 응답 반환
 */
@Slf4j
@Component
public class DeviceAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        String traceId = (String) request.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        log.warn("[{}] Device 인증 실패: {}", traceId, authException.getMessage());

        ApiError apiError = ApiError.builder()
                .code(ErrorCode.AUTH_REQUIRED.name())
                .message(ErrorCode.AUTH_REQUIRED.getMessage())
                .details(null)
                .build();

        ApiResponse<Void> errorResponse = ApiResponse.fail(apiError, traceId);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}