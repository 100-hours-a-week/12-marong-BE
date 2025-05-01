package com.ktb.marong.service.survey;

import com.ktb.marong.domain.survey.SurveyDislikedFood;
import com.ktb.marong.domain.survey.SurveyHobby;
import com.ktb.marong.domain.survey.SurveyLikedFood;
import com.ktb.marong.domain.survey.SurveyMBTI;
import com.ktb.marong.domain.user.User;
import com.ktb.marong.dto.request.survey.SurveyRequestDto;
import com.ktb.marong.dto.response.survey.SurveyResponseDto;
import com.ktb.marong.exception.CustomException;
import com.ktb.marong.exception.ErrorCode;
import com.ktb.marong.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SurveyService {

    private final UserRepository userRepository;
    private final SurveyMBTIRepository surveyMBTIRepository;
    private final SurveyHobbyRepository surveyHobbyRepository;
    private final SurveyLikedFoodRepository surveyLikedFoodRepository;
    private final SurveyDislikedFoodRepository surveyDislikedFoodRepository;

    /**
     * 사용자 설문 최초 제출
     */
    @Transactional
    public Long saveSurvey(Long userId, SurveyRequestDto requestDto) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 기존 데이터가 있는 경우 모두 삭제 (중복 방지)
        surveyMBTIRepository.deleteAllByUser(user);
        surveyHobbyRepository.deleteAllByUser(user);
        surveyLikedFoodRepository.deleteAllByUser(user);
        surveyDislikedFoodRepository.deleteAllByUser(user);

        // MBTI 정보 저장
        SurveyMBTI mbti = SurveyMBTI.builder()
                .user(user)
                .eiScore(requestDto.getEiScore())
                .snScore(requestDto.getSnScore())
                .tfScore(requestDto.getTfScore())
                .jpScore(requestDto.getJpScore())
                .build();
        surveyMBTIRepository.save(mbti);

        // 취미 정보 저장
        List<SurveyHobby> hobbies = requestDto.getHobbies().stream()
                .map(hobby -> SurveyHobby.builder()
                        .user(user)
                        .hobbyName(hobby)
                        .build())
                .collect(Collectors.toList());
        surveyHobbyRepository.saveAll(hobbies);

        // 좋아하는 음식 정보 저장
        List<SurveyLikedFood> likedFoods = requestDto.getLikedFoods().stream()
                .map(food -> SurveyLikedFood.builder()
                        .user(user)
                        .foodName(food)
                        .build())
                .collect(Collectors.toList());
        surveyLikedFoodRepository.saveAll(likedFoods);

        // 싫어하는 음식 정보 저장
        List<SurveyDislikedFood> dislikedFoods = requestDto.getDislikedFoods().stream()
                .map(food -> SurveyDislikedFood.builder()
                        .user(user)
                        .foodName(food)
                        .build())
                .collect(Collectors.toList());
        surveyDislikedFoodRepository.saveAll(dislikedFoods);

        // 설문 완료 처리
        user.completeInitialSurvey();
        userRepository.save(user);

        return userId;
    }

    /**
     * 사용자 설문 정보 조회
     */
    @Transactional(readOnly = true)
    public SurveyResponseDto getSurvey(Long userId) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // MBTI 정보 조회 (변경된 부분)
        List<SurveyMBTI> mbtiList = surveyMBTIRepository.findAllByUser(user);
        if (mbtiList.isEmpty()) {
            throw new CustomException(ErrorCode.SURVEY_NOT_FOUND, "설문 정보가 존재하지 않습니다.");
        }

        // 가장 최근에 생성된 MBTI 정보를 사용 (ID가 가장 큰 것)
        SurveyMBTI mbti = mbtiList.stream()
                .max(Comparator.comparing(SurveyMBTI::getId))
                .orElseThrow(() -> new CustomException(ErrorCode.SURVEY_NOT_FOUND));

        // 취미 정보 조회
        List<String> hobbies = surveyHobbyRepository.findAllByUser(user).stream()
                .map(SurveyHobby::getHobbyName)
                .collect(Collectors.toList());

        // 좋아하는 음식 정보 조회
        List<String> likedFoods = surveyLikedFoodRepository.findAllByUser(user).stream()
                .map(SurveyLikedFood::getFoodName)
                .collect(Collectors.toList());

        // 싫어하는 음식 정보 조회
        List<String> dislikedFoods = surveyDislikedFoodRepository.findAllByUser(user).stream()
                .map(SurveyDislikedFood::getFoodName)
                .collect(Collectors.toList());

        return SurveyResponseDto.builder()
                .eiScore(mbti.getEiScore())
                .snScore(mbti.getSnScore())
                .tfScore(mbti.getTfScore())
                .jpScore(mbti.getJpScore())
                .hobbies(hobbies)
                .likedFoods(likedFoods)
                .dislikedFoods(dislikedFoods)
                .build();
    }

    /**
     * 사용자 설문 정보 수정
     */
    @Transactional
    public Long updateSurvey(Long userId, SurveyRequestDto requestDto) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 기존 설문 정보 존재 여부 확인
        List<SurveyMBTI> mbtiList = surveyMBTIRepository.findAllByUser(user);
        if (mbtiList.isEmpty()) {
            throw new CustomException(ErrorCode.SURVEY_NOT_FOUND, "기존 설문 정보가 존재하지 않습니다.");
        }

        // 기존 설문 데이터 삭제 (수정 전 명시적 삭제)
        surveyMBTIRepository.deleteAllByUser(user);
        // 삭제 후 flush를 추가해 삭제 작업이 즉시 데이터베이스에 반영되도록 함
        surveyMBTIRepository.flush();

        surveyHobbyRepository.deleteAllByUser(user);
        surveyHobbyRepository.flush();

        surveyLikedFoodRepository.deleteAllByUser(user);
        surveyLikedFoodRepository.flush();

        surveyDislikedFoodRepository.deleteAllByUser(user);
        surveyDislikedFoodRepository.flush();

        // 새로운 MBTI 정보 저장
        SurveyMBTI mbti = SurveyMBTI.builder()
                .user(user)
                .eiScore(requestDto.getEiScore())
                .snScore(requestDto.getSnScore())
                .tfScore(requestDto.getTfScore())
                .jpScore(requestDto.getJpScore())
                .build();
        surveyMBTIRepository.save(mbti);

        // 새로운 취미 정보 저장
        List<SurveyHobby> hobbies = requestDto.getHobbies().stream()
                .map(hobby -> SurveyHobby.builder()
                        .user(user)
                        .hobbyName(hobby)
                        .build())
                .collect(Collectors.toList());
        surveyHobbyRepository.saveAll(hobbies);

        // 새로운 좋아하는 음식 정보 저장
        List<SurveyLikedFood> likedFoods = requestDto.getLikedFoods().stream()
                .map(food -> SurveyLikedFood.builder()
                        .user(user)
                        .foodName(food)
                        .build())
                .collect(Collectors.toList());
        surveyLikedFoodRepository.saveAll(likedFoods);

        // 새로운 싫어하는 음식 정보 저장
        List<SurveyDislikedFood> dislikedFoods = requestDto.getDislikedFoods().stream()
                .map(food -> SurveyDislikedFood.builder()
                        .user(user)
                        .foodName(food)
                        .build())
                .collect(Collectors.toList());
        surveyDislikedFoodRepository.saveAll(dislikedFoods);

        return userId;
    }
}