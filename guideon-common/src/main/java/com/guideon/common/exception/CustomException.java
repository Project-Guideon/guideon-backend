package com.guideon.common.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;
    /**
     * 클라이언트에게 표시할 상세 메시지
     * errorCode의 기본 메시지를 오버라이드함
     * null일 경우 errorCode.getMessage() 사용
     */
    private final String message;

    /**
     * 에러 관련 추가 정보 (필요없으면 뺴도 무관)
     * ApiError의 details 필드에 포함됨
     */
    private final Map<String, Object> details;

    //기본 생성자 : ErrorCode만으로 예외 생성, 메시지는 ErrorCode의 기본 메시지 사용
    public CustomException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage(), null);
    }

    //커스텀 메시지와 함께 예외 생성
    public CustomException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    //전체 정보와 함께 예외 생성
    public CustomException(ErrorCode errorCode, String message, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.message = message;
        this.details = details;
    }

    //RuntimeException의 getMessage()가 this.message를 반환하도록 보장
    @Override
    public String getMessage() {
        return message;
    }
}
