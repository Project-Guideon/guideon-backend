package com.guideon.guideonbackend.global.trace;

import java.util.UUID;

//TraceIdFilter → 요청마다 trace_id 생성/추출 → MDC/Request에 저장 → 로그/응답에 포함
public final class TraceIdUtil {
    //Request Attribute에 저장할 때 사용하는 키
    public static final String TRACE_ID_ATTR = "TRACE_ID";
    //HTTP 요청/응답 헤더 이름
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    //SLF4J MDC(Mapped Diagnostic Context) 키
    public static final String MDC_KEY = "trace_id";

    private TraceIdUtil() {}

    //새로운 trace_id 생성
    //UUID v4 형식 사용
    public static String newTraceId() {
        return UUID.randomUUID().toString();
    }
}