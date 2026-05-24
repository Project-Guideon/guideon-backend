package com.guideon.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // ── 공통 ──
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "입력값 검증 실패"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스 없음(일반)"),
    CONFLICT(HttpStatus.CONFLICT, "유니크 충돌/중복(unique constraint conflict)"),
    DOMAIN_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "도메인 규칙 위반(폴리곤(polygon) 포함 등)"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "과다 요청(rate limited)"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류"),
    UPSTREAM_TIMEOUT(HttpStatus.SERVICE_UNAVAILABLE, "외부/AI 의존 장애(upstream timeout/outage)"),

    // ── 인증/인가 ──
    AUTH_REQUIRED(HttpStatus.UNAUTHORIZED, "인증 필요(토큰 없음)"),
    AUTH_INVALID(HttpStatus.UNAUTHORIZED, "인증 실패(토큰 불일치/만료)"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한 없음"),
    INVITE_EXPIRED(HttpStatus.GONE, "초대 만료"),
    INVITE_ALREADY_USED(HttpStatus.GONE, "이미 사용된 초대"),

    // ── Site ──
    ADMIN_SITE_FORBIDDEN(HttpStatus.FORBIDDEN, "관리자 site 스코프(scope) 위반"),
    SITE_NOT_FOUND(HttpStatus.NOT_FOUND, "사이트를 찾을 수 없습니다"),
    SITE_INACTIVE(HttpStatus.FORBIDDEN, "site 비활성(kill switch)"),

    // ── Zone ──
    ZONE_CODE_DUPLICATE(HttpStatus.CONFLICT, "구역 코드 중복"),
    ZONE_PARENT_REQUIRED(HttpStatus.BAD_REQUEST, "SUB 구역은 부모 구역이 필수입니다"),
    ZONE_SUB_OUTSIDE_PARENT(HttpStatus.UNPROCESSABLE_ENTITY, "SUB 폴리곤이 부모 INNER 영역 밖에 있습니다"),
    ZONE_SUB_OVERLAP_FORBIDDEN(HttpStatus.UNPROCESSABLE_ENTITY, "SUB 구역이 동일 부모 내 다른 SUB와 겹칩니다"),

    // ── Place ──
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "장소를 찾을 수 없습니다"),
    ZONE_NOT_FOUND(HttpStatus.NOT_FOUND, "구역을 찾을 수 없습니다"),

    // ── Document ──
    DOC_HASH_DUPLICATE(HttpStatus.CONFLICT, "동일한 파일이 이미 업로드되어 있습니다"),
    DOC_NOT_FOUND(HttpStatus.NOT_FOUND, "문서를 찾을 수 없습니다"),
    DOC_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다"),

    // ── Mascot ──
    MASCOT_NOT_FOUND(HttpStatus.NOT_FOUND, "마스코트 설정을 찾을 수 없습니다"),
    MASCOT_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 마스코트가 등록된 관광지입니다"),
    MASCOT_GENERATION_NOT_FOUND(HttpStatus.NOT_FOUND, "마스코트 생성 작업을 찾을 수 없습니다"),
    MASCOT_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "3D 모델 생성에 실패했습니다"),
    TRIPO_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "Tripo AI API 호출에 실패했습니다"),
    VOICE_CLONE_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "음성 클로닝에 실패했습니다"),

    // ── Device ──
    DEVICE_NOT_FOUND(HttpStatus.NOT_FOUND, "디바이스를 찾을 수 없습니다"),
    DEVICE_ID_DUPLICATE(HttpStatus.CONFLICT, "이미 사용 중인 디바이스 ID입니다"),
    DEVICE_INACTIVE(HttpStatus.FORBIDDEN, "비활성화된 디바이스입니다"),

    // ── Chat ──
    CHAT_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅 세션을 찾을 수 없습니다"),
    CHAT_SESSION_ENDED(HttpStatus.CONFLICT, "이미 종료된 채팅 세션입니다"),

    // ── Pairing ──
    PAIRING_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "페어링 코드를 찾을 수 없습니다"),
    PAIRING_CODE_EXPIRED(HttpStatus.GONE, "만료된 페어링 코드입니다"),
    PAIRING_CODE_ALREADY_PAIRED(HttpStatus.CONFLICT, "이미 매칭 완료된 페어링 코드입니다"),
    PAIRING_ALREADY_CLAIMED(HttpStatus.CONFLICT, "이미 토큰이 수령된 페어링입니다"),
    PAIRING_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "페어링 인증 실패(Nonce 불일치)");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
