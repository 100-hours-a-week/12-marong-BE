package com.ktb.marong.dto.response.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * API 응답을 위한 공통 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private T data;
    private String message;
    private Map<String, Object> meta;

    /**
     * 성공 응답 생성
     */
    public static <T> ApiResponse<T> success(T data, String message, Map<String, Object> meta) {
        return ApiResponse.<T>builder()
                .data(data)
                .message(message)
                .meta(meta)
                .build();
    }

    /**
     * 에러 응답 생성
     */
    public static ApiResponse<Object> error(String code, String message) {
        return ApiResponse.builder()
                .data(null)
                .message(message)
                .meta(Map.of("code", code))
                .build();
    }
}