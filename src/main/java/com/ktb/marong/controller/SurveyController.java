package com.ktb.marong.controller;

import com.ktb.marong.dto.request.survey.SurveyRequestDto;
import com.ktb.marong.dto.response.common.ApiResponse;
import com.ktb.marong.dto.response.survey.SurveyResponseDto;
import com.ktb.marong.security.CurrentUser;
import com.ktb.marong.service.survey.SurveyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 설문조사 관련 API 컨트롤러
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/survey")
public class SurveyController {

    private final SurveyService surveyService;

    /**
     * 사용자 설문 최초 제출
     */
    @PostMapping
    public ResponseEntity<?> saveSurvey(@CurrentUser Long userId, @Valid @RequestBody SurveyRequestDto requestDto) {
        log.info("설문 저장 요청: userId={}", userId);
        Long savedUserId = surveyService.saveSurvey(userId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                        savedUserId,
                        "survey_saved",
                        null
                )
        );
    }

    /**
     * 사용자 설문 정보 조회
     */
    @GetMapping
    public ResponseEntity<?> getSurvey(@CurrentUser Long userId) {
        log.info("설문 조회 요청: userId={}", userId);
        SurveyResponseDto response = surveyService.getSurvey(userId);
        return ResponseEntity.ok(
                ApiResponse.success(
                        response,
                        "survey_retrieved",
                        null
                )
        );
    }

    /**
     * 사용자 설문 정보 수정
     */
    @PutMapping
    public ResponseEntity<?> updateSurvey(@CurrentUser Long userId, @Valid @RequestBody SurveyRequestDto requestDto) {
        log.info("설문 수정 요청: userId={}", userId);
        Long updatedUserId = surveyService.updateSurvey(userId, requestDto);
        return ResponseEntity.ok(
                ApiResponse.success(
                        updatedUserId,
                        "survey_updated",
                        null
                )
        );
    }
}