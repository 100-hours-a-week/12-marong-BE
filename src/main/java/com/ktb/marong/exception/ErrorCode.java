package com.ktb.marong.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 에러 코드 열거형
 * 서비스 전반에서 사용되는 에러 코드를 정의
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 공통 에러
    INTERNAL_SERVER_ERROR(500, "서버 오류입니다."),

    // 인증 관련 에러
    UNAUTHORIZED(401, "인증이 필요합니다."),
    INVALID_TOKEN(403, "유효하지 않거나 만료된 토큰입니다."),
    TOKEN_EXPIRED(403, "토큰이 만료되었습니다."),
    INVALID_REFRESH_TOKEN(403, "유효하지 않은 refreshToken 입니다."),
    MISSING_TOKEN(400, "refreshToken이 필요합니다."),

    // 사용자 관련 에러
    USER_NOT_FOUND(404, "사용자 조회에 실패하였습니다."),

    // OAuth 관련 에러
    INVALID_PROVIDER(400, "provider 값이 올바르지 않습니다."),
    INVALID_KAKAO_CODE(400, "인가 코드가 유효하지 않습니다."),
    SOCIAL_AUTH_FAILED(401, "소셜 인증에 실패했습니다.");

    private final int status;
    private final String message;
}