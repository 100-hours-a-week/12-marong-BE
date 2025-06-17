package com.ktb.marong.exception;

import lombok.Getter;

/**
 * 커스텀 예외 클래스
 * 서비스 로직에서 발생하는 예외를 처리하기 위한 클래스
 */
@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public CustomException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}