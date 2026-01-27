package com.guideon.guideonbackend.global.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class TraceIdFilter extends OncePerRequestFilter {
    //OncePerRequestFilter로 하나의 요청에 대해 필터가 정확히 한 번만 실행됨을 보장시켜줌
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 요청 헤더에서 trace_id 확인
        // 클라이언트가 이미 trace_id를 보낸 경우 재사용
        // 분산 시스템에서 여러 서비스를 거치는 요청을 동일한 trace_id로 추적
        String traceId = request.getHeader(TraceIdUtil.TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            // trace_id가 없으면 새로 생성
            traceId = TraceIdUtil.newTraceId();
        }

        // Request Attribute에 저장
        // Controller, Service, GlobalExceptionHandler 등에서 꺼내 쓸 수 있음
        // 사용 예: String id = (String) request.getAttribute(TraceIdUtil.TRACE_ID_ATTR);
        request.setAttribute(TraceIdUtil.TRACE_ID_ATTR, traceId);

        // Response Header에 추가
        // 클라이언트가 응답에서 trace_id를 확인하여 문제 보고 시 함께 전달
        response.setHeader(TraceIdUtil.TRACE_ID_HEADER, traceId);

        // MDC(Mapped Diagnostic Context)에 저장
        // SLF4J/Logback에서 제공하는 ThreadLocal 기반 컨텍스트 저장소
        // 이후 이 요청 처리 중 발생하는 모든 로그에 trace_id가 자동으로 포함됨
        MDC.put(TraceIdUtil.MDC_KEY, traceId);
        // validateOrder(orderId, traceId);  << 이런것과 같이 traceid 계속 안쓰기 위함

        try {
            //다음 필터 또는 컨트롤러로 요청 전달
            filterChain.doFilter(request, response);
        } finally {
            //  finally로 MDC 정리
            //  MDC는 ThreadLocal 기반이므로 요청 처리 완료 후 반드시 제거해야 함
            // 톰캣처럼 스레드를 재사용하는 환경에서 이전 요청의 trace_id가 남아있으면 안 됨
            // finally 블록에서 처리하여 예외 발생 시에도 정리 보장
            MDC.remove(TraceIdUtil.MDC_KEY);
        }
    }
}
