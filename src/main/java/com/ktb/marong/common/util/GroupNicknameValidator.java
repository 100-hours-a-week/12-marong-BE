package com.ktb.marong.common.util;

import com.ktb.marong.exception.CustomException;
import com.ktb.marong.exception.ErrorCode;

import java.util.regex.Pattern;

/**
 * 그룹 내 닉네임 유효성 검증 유틸리티
 */
public class GroupNicknameValidator {

    private static final int MIN_NICKNAME_LENGTH = 2;
    private static final int MAX_NICKNAME_LENGTH = 20;
    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[가-힣a-zA-Z0-9._\\-\\s()]{2,20}$");

    /**
     * 닉네임 기본 형식 검증
     */
    public static void validateNicknameFormat(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new CustomException(ErrorCode.GROUP_NICKNAME_REQUIRED, "그룹 내 닉네임은 필수입니다.");
        }

        String trimmedNickname = nickname.trim();

        if (trimmedNickname.length() < MIN_NICKNAME_LENGTH) {
            throw new CustomException(ErrorCode.NICKNAME_TOO_SHORT, "닉네임은 최소 " + MIN_NICKNAME_LENGTH + "자 이상이어야 합니다.");
        }

        if (trimmedNickname.length() > MAX_NICKNAME_LENGTH) {
            throw new CustomException(ErrorCode.NICKNAME_TOO_LONG, "닉네임은 최대 " + MAX_NICKNAME_LENGTH + "자까지 입력 가능합니다.");
        }

        if (!NICKNAME_PATTERN.matcher(trimmedNickname).matches()) {
            throw new CustomException(ErrorCode.INVALID_NICKNAME_FORMAT,
                    "닉네임은 한글, 영문, 숫자, 점(.), 언더스코어(_), 하이픈(-), 공백, 괄호()만 사용 가능합니다.");
        }

        // 공백만으로 이루어진 닉네임 방지
        if (trimmedNickname.replaceAll("\\s", "").isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_NICKNAME_FORMAT, "공백만으로 이루어진 닉네임은 사용할 수 없습니다.");
        }
    }

    /**
     * 닉네임 정규화 (앞뒤 공백 제거, 연속 공백 하나로 압축)
     */
    public static String normalizeNickname(String nickname) {
        if (nickname == null) {
            return null;
        }

        // 앞뒤 공백 제거 및 연속된 공백을 하나로 압축
        return nickname.trim().replaceAll("\\s+", " ");
    }
}