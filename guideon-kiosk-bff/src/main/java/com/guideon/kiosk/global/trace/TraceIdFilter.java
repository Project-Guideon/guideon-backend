package com.guideon.kiosk.global.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String traceId = request.getHeader(TraceIdUtil.TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = TraceIdUtil.newTraceId();
        }

        request.setAttribute(TraceIdUtil.TRACE_ID_ATTR, traceId);
        response.setHeader(TraceIdUtil.TRACE_ID_HEADER, traceId);
        MDC.put(TraceIdUtil.MDC_KEY, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TraceIdUtil.MDC_KEY);
        }
    }
}