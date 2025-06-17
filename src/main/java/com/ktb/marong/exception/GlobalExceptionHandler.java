package com.ktb.marong.exception;

import com.ktb.marong.dto.response.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 핸들러
 * 컨트롤러에서 발생하는 예외를 처리하는 클래스
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * CustomException 처리
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Object>> handleCustomException(CustomException e) {
        log.error("CustomException 발생: {}", e.getMessage());

        ErrorCode errorCode = e.getErrorCode();
        ApiResponse<Object> response = ApiResponse.error(errorCode.name(), e.getMessage());

        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    /**
     * 유효성 검증 예외 처리
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResponse<Object>> handleValidationException(Exception e) {
        log.error("유효성 검증 예외 발생: {}", e.getMessage());

        Map<String, String> errorFields = new HashMap<>();

        if (e instanceof MethodArgumentNotValidException) {
            ((MethodArgumentNotValidException) e).getBindingResult().getFieldErrors()
                    .forEach(error -> errorFields.put(error.getField(), error.getDefaultMessage()));
        } else if (e instanceof BindException) {
            ((BindException) e).getBindingResult().getFieldErrors()
                    .forEach(error -> errorFields.put(error.getField(), error.getDefaultMessage()));
        }

        Map<String, Object> meta = new HashMap<>();
        meta.put("code", "INVALID_INPUT");
        meta.put("errors", errorFields);

        ApiResponse<Object> response = ApiResponse.builder()
                .data(null)
                .message("입력값이 올바르지 않습니다.")
                .meta(meta)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 기타 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        log.error("예외 발생: {}", e.getMessage(), e);

        ApiResponse<Object> response = ApiResponse.error("INTERNAL_SERVER_ERROR", "서버 오류입니다.");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}