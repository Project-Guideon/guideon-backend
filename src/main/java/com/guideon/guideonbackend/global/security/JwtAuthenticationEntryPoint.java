package com.guideon.guideonbackend.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guideon.guideonbackend.global.exception.ErrorCode;
import com.guideon.guideonbackend.global.response.ApiError;
import com.guideon.guideonbackend.global.response.ApiResponse;
import com.guideon.guideonbackend.global.trace.TraceIdUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

   //인증 실패시 호출
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {

        // Trace ID 추출
        String traceId = (String) request.getAttribute(TraceIdUtil.TRACE_ID_ATTR);

        log.warn("[{}] Authentication failed: {}", traceId, authException.getMessage());

        // 에러 응답 생성
        ApiError apiError = ApiError.builder()
                .code(ErrorCode.AUTH_REQUIRED.name())
                .message(ErrorCode.AUTH_REQUIRED.getMessage())
                .details(null)
                .build();

        ApiResponse<Void> errorResponse = ApiResponse.fail(apiError, traceId);

        // HTTP 응답 설정
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // JSON 응답 작성
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
