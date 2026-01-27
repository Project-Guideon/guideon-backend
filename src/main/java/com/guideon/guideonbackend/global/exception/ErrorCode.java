package com.guideon.guideonbackend.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "입력값 검증 실패"),
    AUTH_REQUIRED(HttpStatus.UNAUTHORIZED, "인증 필요(토큰 없음)"),
    AUTH_INVALID(HttpStatus.UNAUTHORIZED, "인증 실패(토큰 불일치/만료)"),
    ADMIN_SITE_FORBIDDEN(HttpStatus.FORBIDDEN, "관리자 site 스코프(scope) 위반"),
    SITE_INACTIVE(HttpStatus.FORBIDDEN, "site 비활성(kill switch)"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스 없음(일반)"),
    CONFLICT(HttpStatus.CONFLICT, "유니크 충돌/중복(unique constraint conflict)"),
    DOMAIN_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "도메인 규칙 위반(폴리곤(polygon) 포함 등)"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "과다 요청(rate limited)"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류"),
    UPSTREAM_TIMEOUT(HttpStatus.SERVICE_UNAVAILABLE, "외부/AI 의존 장애(upstream timeout/outage)");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
