package com.guideon.kiosk.global.trace;

import java.util.UUID;

public final class TraceIdUtil {
    public static final String TRACE_ID_ATTR = "TRACE_ID";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String MDC_KEY = "trace_id";

    private TraceIdUtil() {}

    public static String newTraceId() {
        return UUID.randomUUID().toString();
    }
}