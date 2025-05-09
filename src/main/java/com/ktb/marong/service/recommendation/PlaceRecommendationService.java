package com.ktb.marong.service.recommendation;

import com.ktb.marong.common.util.WeekCalculator;
import com.ktb.marong.domain.manitto.Manitto;
import com.ktb.marong.domain.recommendation.PlaceRecommendation;
import com.ktb.marong.domain.recommendation.PlaceRecommendationSession;
import com.ktb.marong.domain.user.User;
import com.ktb.marong.dto.response.recommendation.PlaceRecommendationResponseDto;
import com.ktb.marong.exception.CustomException;
import com.ktb.marong.exception.ErrorCode;
import com.ktb.marong.repository.ManittoRepository;
import com.ktb.marong.repository.PlaceRecommendationRepository;
import com.ktb.marong.repository.PlaceRecommendationSessionRepository;
import com.ktb.marong.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceRecommendationService {

    private final UserRepository userRepository;
    private final ManittoRepository manittoRepository;
    private final PlaceRecommendationSessionRepository sessionRepository;
    private final PlaceRecommendationRepository placeRepository;

    /**
     * 장소 추천 조회 (밥집 & 카페)
     * MVP에서는 모든 사용자가 그룹 ID 1에 속한다고 가정
     */
    @Transactional(readOnly = true)
    public PlaceRecommendationResponseDto getPlaceRecommendations(Long userId) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 현재 주차에 해당하는 마니또 매칭 정보 조회 (그룹 ID 1 고정)
        int currentWeek = WeekCalculator.getCurrentWeek();
        // findByGiverIdAndGroupIdAndWeek를 findByManitteeIdAndGroupIdAndWeek로 변경
        List<Manitto> manittoList = manittoRepository.findByManitteeIdAndGroupIdAndWeek(userId, 1L, currentWeek);

        // 마니또 매칭이 없는 경우 예외 발생
        if (manittoList.isEmpty()) {
            throw new CustomException(ErrorCode.MANITTO_NOT_FOUND, "마니또 매칭 정보가 없어 장소 추천을 제공할 수 없습니다.");
        }

        // 매칭된 마니또 정보 (첫 번째 매칭 사용)
        Manitto manitto = manittoList.get(0);

        // 현재 주차에 해당하는 장소 추천 세션 조회
        List<PlaceRecommendationSession> sessions = sessionRepository.findByManitteeIdAndWeek(userId, currentWeek);

        // 세션이 없는 경우 예외 발생
        if (sessions.isEmpty()) {
            throw new CustomException(ErrorCode.RECOMMENDATION_NOT_FOUND, "장소 추천 정보가 없습니다.");
        }

        // 현재 세션 (첫 번째 세션 사용)
        PlaceRecommendationSession session = sessions.get(0);

        // 레스토랑 목록 조회 및 DTO 변환
        List<PlaceRecommendationResponseDto.PlaceDto> restaurants = getRandomPlaces(session.getId(), "restaurant");

        // 카페 목록 조회 및 DTO 변환
        List<PlaceRecommendationResponseDto.PlaceDto> cafes = getRandomPlaces(session.getId(), "cafe");

        // 응답 생성
        return PlaceRecommendationResponseDto.builder()
                .restaurants(restaurants)
                .cafes(cafes)
                .build();
    }

    /**
     * 장소 타입에 따라 랜덤 장소 목록 조회
     * 최대 1개만 반환하도록 처리
     */
    private List<PlaceRecommendationResponseDto.PlaceDto> getRandomPlaces(Long sessionId, String type) {
        List<PlaceRecommendation> places = placeRepository.findBySessionIdAndType(sessionId, type);

        // 장소가 없는 경우 빈 목록 반환
        if (places.isEmpty()) {
            return Collections.emptyList();
        }

        // 랜덤으로 1개 선택
        Random random = new Random();
        PlaceRecommendation randomPlace = places.get(random.nextInt(places.size()));

        // DTO 변환
        PlaceRecommendationResponseDto.PlaceDto placeDto = convertToPlaceDto(randomPlace);

        // 단일 아이템 리스트로 반환
        return Collections.singletonList(placeDto);
    }

    /**
     * 장소 엔티티를 DTO로 변환
     */
    private PlaceRecommendationResponseDto.PlaceDto convertToPlaceDto(PlaceRecommendation place) {
        return PlaceRecommendationResponseDto.PlaceDto.builder()
                .name(place.getName())
                .category(place.getCategory())
                .hours(place.getOpeningHours())
                .address(place.getAddress())
                .build();
    }
}