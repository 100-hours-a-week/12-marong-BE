package com.ktb.marong.common.util;

import com.ktb.marong.exception.CustomException;
import com.ktb.marong.exception.ErrorCode;

/**
 * 그룹 생성 관련 유효성 검증 유틸리티
 */
public class GroupValidator {

    private static final int MAX_GROUP_NAME_LENGTH = 10;
    private static final int MAX_GROUP_DESCRIPTION_LENGTH = 30;

    /**
     * 그룹 이름 유효성 검증
     */
    public static void validateGroupName(String groupName) {
        if (groupName == null || groupName.trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "그룹 이름은 필수입니다.");
        }

        String trimmedName = groupName.trim();

        if (trimmedName.length() > MAX_GROUP_NAME_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_INPUT,
                    "그룹 이름은 최대 " + MAX_GROUP_NAME_LENGTH + "자까지 입력 가능합니다.");
        }

        // 공백만으로 이루어진 그룹명 방지
        if (trimmedName.replaceAll("\\s", "").isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "공백만으로 이루어진 그룹 이름은 사용할 수 없습니다.");
        }
    }

    /**
     * 그룹 설명 유효성 검증
     */
    public static void validateGroupDescription(String description) {
        if (description != null && description.trim().length() > MAX_GROUP_DESCRIPTION_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_INPUT,
                    "그룹 설명은 최대 " + MAX_GROUP_DESCRIPTION_LENGTH + "자까지 입력 가능합니다.");
        }
    }

    /**
     * 그룹 이름 정규화 (앞뒤 공백 제거, 연속 공백 하나로 압축)
     */
    public static String normalizeGroupName(String groupName) {
        if (groupName == null) {
            return null;
        }
        return groupName.trim().replaceAll("\\s+", " ");
    }

    /**
     * 그룹 설명 정규화 (앞뒤 공백 제거)
     */
    public static String normalizeGroupDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return null;
        }
        return description.trim();
    }
}