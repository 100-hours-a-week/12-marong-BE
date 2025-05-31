package com.ktb.marong.common.util;

import com.ktb.marong.exception.CustomException;
import com.ktb.marong.exception.ErrorCode;

import java.util.regex.Pattern;

public class InviteCodeValidator {

    // 영어만 포함하는지 확인하는 정규식
    private static final Pattern ONLY_LETTERS_PATTERN = Pattern.compile("^[A-Z]+$");

    // 숫자만 포함하는지 확인하는 정규식
    private static final Pattern ONLY_DIGITS_PATTERN = Pattern.compile("^[0-9]+$");

    /**
     * 초대 코드 유효성 검증
     * - 정확히 6자리
     * - 영어 대문자, 소문자, 숫자만 허용 (대소문자 구분 안함)
     * - 영어와 숫자 혼합 필수 (영어만 또는 숫자만은 불가)
     * - 특수문자, 한글, 공백 등 불허
     */
    public static void validateInviteCode(String inviteCode) {
        if (inviteCode == null || inviteCode.trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INVITE_CODE, "초대 코드는 필수입니다.");
        }

        // 공백 제거 후 대소문자 통일 (대문자로 변환)
        String normalizedCode = inviteCode.trim().toUpperCase();

        // 길이 검증
        if (normalizedCode.length() < 6) {
            throw new CustomException(ErrorCode.INVITE_CODE_TOO_SHORT);
        }

        if (normalizedCode.length() > 6) {
            throw new CustomException(ErrorCode.INVITE_CODE_TOO_LONG);
        }

        // 기본 형식 검증 (영어, 숫자만 허용) - 대문자로 통일된 상태에서 검증
        if (!Pattern.matches("^[A-Z0-9]{6}$", normalizedCode)) {
            throw new CustomException(ErrorCode.INVITE_CODE_INVALID_CHARACTERS);
        }

        // 영어와 숫자 혼합 여부 검증 (영어만 또는 숫자만인 경우 거부)
        if (ONLY_LETTERS_PATTERN.matcher(normalizedCode).matches()) {
            throw new CustomException(ErrorCode.INVALID_INVITE_CODE_FORMAT,
                    "초대 코드는 영어와 숫자를 모두 포함해야 합니다. (영어만 사용 불가)");
        }

        if (ONLY_DIGITS_PATTERN.matcher(normalizedCode).matches()) {
            throw new CustomException(ErrorCode.INVALID_INVITE_CODE_FORMAT,
                    "초대 코드는 영어와 숫자를 모두 포함해야 합니다. (숫자만 사용 불가)");
        }
    }

    /**
     * 초대 코드 정규화 (앞뒤 공백 제거 및 대문자 변환)
     */
    public static String normalizeInviteCode(String inviteCode) {
        if (inviteCode == null) {
            return null;
        }
        return inviteCode.trim().toUpperCase();
    }
}