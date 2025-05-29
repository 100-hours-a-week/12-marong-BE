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
    SOCIAL_AUTH_FAILED(401, "소셜 인증에 실패했습니다."),

    // 설문조사 관련 에러
    SURVEY_NOT_FOUND(404, "설문 정보가 존재하지 않습니다."),
    INVALID_SURVEY_FORMAT(400, "입력 형식이 잘못되었습니다."),
    SURVEY_ALREADY_SUBMITTED(409, "이미 최초 설문 조사를 제출한 사용자입니다."),

    // 게시글 관련 에러
    FEED_NOT_FOUND(404, "게시글을 찾을 수 없습니다."),
    MISSION_ALREADY_COMPLETED(409, "이미 해당 미션을 완료했습니다."),
    ANONYMOUS_NAME_NOT_FOUND(404, "익명 이름을 찾을 수 없습니다."),
    ALREADY_LIKED(409, "이미 좋아요한 게시글입니다."),
    NOT_LIKED(409, "좋아요하지 않은 게시글입니다."),

    // 파일 관련 에러
    EMPTY_FILE(400, "빈 파일은 업로드할 수 없습니다."),
    INVALID_FILE_FORMAT(400, "지원하지 않는 파일 형식입니다."),
    FILE_TOO_LARGE(413, "업로드 가능한 파일 크기를 초과했습니다."),
    FILE_UPLOAD_ERROR(500, "파일 업로드에 실패했습니다."),

    // 미션 관련 에러
    MISSION_NOT_FOUND(404, "미션을 찾을 수 없습니다."),
    DAILY_MISSION_LIMIT_EXCEEDED(400, "하루에 한 개의 미션만 수행할 수 있습니다."),
    MISSION_NOT_ASSIGNED(400, "할당되지 않은 미션입니다."),

    // 마니또 관련 에러
    MANITTO_NOT_FOUND(404, "현재 매칭된 마니띠가 없습니다."),
    MISSION_STATUS_NOT_FOUND(404, "진행 중인 미션이 없습니다."),

    // 추천 관련 에러
    RECOMMENDATION_NOT_FOUND(404, "추천 정보를 찾을 수 없습니다."),

    // 그룹 관련 에러
    MAX_GROUPS_EXCEEDED(400, "사용자당 최대 4개의 그룹까지 생성/가입할 수 있습니다."),
    GROUP_MEMBER_LIMIT_EXCEEDED(400, "그룹당 최대 150명까지 가입할 수 있습니다."),
    GROUP_NAME_DUPLICATED(409, "동일한 그룹 이름이 이미 존재합니다."),
    INVITE_CODE_DUPLICATED(409, "초대코드가 이미 사용 중입니다. 다른 코드를 입력해주세요."),
    GROUP_NOT_FOUND(404, "해당 그룹을 찾을 수 없습니다."),
    INVALID_INVITE_CODE(400, "유효하지 않은 초대코드입니다."),
    INVITE_CODE_MISMATCH(400, "초대코드가 일치하지 않습니다. 다시 확인해주세요."),
    INVALID_INVITE_CODE_FORMAT(400, "초대 코드는 영어와 숫자를 포함한 정확히 6자리여야 합니다."),
    INVITE_CODE_TOO_SHORT(400, "초대 코드가 너무 짧습니다. 6자리로 입력해주세요."),
    INVITE_CODE_TOO_LONG(400, "초대 코드가 너무 깁니다. 6자리로 입력해주세요."),
    INVITE_CODE_INVALID_CHARACTERS(400, "초대 코드는 영어와 숫자만 사용할 수 있습니다."),
    ALREADY_JOINED_GROUP(409, "이미 가입된 그룹입니다."),
    GROUP_NICKNAME_REQUIRED(400, "그룹 내 사용자 닉네임 설정이 필요합니다."),

    // 닉네임 관련 에러 (새로 추가)
    NICKNAME_TOO_SHORT(400, "닉네임이 너무 짧습니다."),
    NICKNAME_TOO_LONG(400, "닉네임이 너무 깁니다."),
    INVALID_NICKNAME_FORMAT(400, "유효하지 않은 닉네임 형식입니다."),
    NICKNAME_DUPLICATED_IN_GROUP(409, "해당 그룹에서 이미 사용 중인 닉네임입니다.");

    private final int status;
    private final String message;
}