package com.ktb.marong.service.recommendation;

import com.ktb.marong.common.util.WeekCalculator;
import com.ktb.marong.domain.group.Group;
import com.ktb.marong.domain.manitto.Manitto;
import com.ktb.marong.domain.recommendation.PlaceRecommendation;
import com.ktb.marong.domain.recommendation.PlaceRecommendationSession;
import com.ktb.marong.domain.user.User;
import com.ktb.marong.dto.response.recommendation.PlaceRecommendationResponseDto;
import com.ktb.marong.exception.CustomException;
import com.ktb.marong.exception.ErrorCode;
import com.ktb.marong.repository.*;
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
    private final UserGroupRepository userGroupRepository;
    private final ManittoRepository manittoRepository;
    private final PlaceRecommendationSessionRepository sessionRepository;
    private final PlaceRecommendationRepository placeRepository;
    private final GroupRepository groupRepository;

    /**
     * 장소 추천 조회 (밥집 & 카페) - 그룹별 분리
     */
    @Transactional(readOnly = true)
    public PlaceRecommendationResponseDto getPlaceRecommendations(Long userId, Long groupId) {
        log.info("장소 추천 조회 시작: userId={}, groupId={}", userId, groupId);

        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 그룹 존재 여부 확인 (추가)
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        // 3. 사용자가 해당 그룹에 속해있는지 확인
        if (!userGroupRepository.existsByUserIdAndGroupId(userId, groupId)) {
            throw new CustomException(ErrorCode.GROUP_NOT_FOUND,
                    "해당 그룹에 속하지 않은 사용자입니다.");
        }

        // 4. 현재 주차 계산
        int currentWeek = WeekCalculator.getCurrentWeek();
        log.info("현재 주차: {}", currentWeek);

        // 5. 현재 주차에 해당하는 마니또 매칭 정보 조회 (그룹별)
        List<Manitto> manittoList = manittoRepository.findByManittoIdAndGroupIdAndWeek(userId, groupId, currentWeek);

        if (manittoList.isEmpty()) {
            log.warn("마니또 매칭 없음: userId={}, groupId={}, week={}", userId, groupId, currentWeek);
            throw new CustomException(ErrorCode.MANITTO_NOT_FOUND,
                    String.format("해당 그룹(ID: %d)에서 마니또 매칭 정보가 없어 장소 추천을 제공할 수 없습니다.", groupId));
        }

        // 6. 매칭된 마니또 정보
        Manitto manitto = manittoList.get(0);
        Long manitteeId = manitto.getManittee().getId();
        log.info("마니또 매칭 정보: manittoId={}, manitteeId={}, groupId={}, week={}",
                userId, manitteeId, groupId, currentWeek);

        // 7. 장소 추천 세션 조회 - 마니또-마니띠 쌍으로 조회
        List<PlaceRecommendationSession> sessions = sessionRepository.findByManittoIdAndManitteeIdAndWeek(
                userId, manitteeId, currentWeek);

        if (sessions.isEmpty()) {
            log.warn("장소 추천 세션 없음: manittoId={}, manitteeId={}, week={}", userId, manitteeId, currentWeek);
            throw new CustomException(ErrorCode.RECOMMENDATION_NOT_FOUND,
                    String.format("마니또-마니띠 매칭(마니또: %d → 마니띠: %d)에 대한 %d주차 장소 추천 정보가 없습니다.",
                            userId, manitteeId, currentWeek));
        }

        // 8. 현재 세션
        PlaceRecommendationSession session = sessions.get(0);
        log.info("장소 추천 세션 정보: sessionId={}, manittoId={}, manitteeId={}, week={}",
                session.getId(), session.getManitto().getId(), session.getManittee().getId(), currentWeek);

        // 9. 레스토랑 목록 조회 및 DTO 변환
        List<PlaceRecommendationResponseDto.PlaceDto> restaurants = getRandomPlaces(session.getId(), "restaurant");

        // 10. 카페 목록 조회 및 DTO 변환
        List<PlaceRecommendationResponseDto.PlaceDto> cafes = getRandomPlaces(session.getId(), "cafe");

        log.info("장소 추천 조회 완료: userId={}, groupId={}, sessionId={}, restaurants={}, cafes={}",
                userId, groupId, session.getId(), restaurants.size(), cafes.size());

        // 11. 응답 생성
        return PlaceRecommendationResponseDto.builder()
                .restaurants(restaurants)
                .cafes(cafes)
                .build();
    }

    /**
     * MVP 호환성을 위한 기본 그룹 장소 추천
     */
    @Deprecated
    @Transactional(readOnly = true)
    public PlaceRecommendationResponseDto getPlaceRecommendations(Long userId) {
        log.warn("기본 메소드 호출 - 기본 그룹(ID: 1) 사용: userId={}", userId);
        return getPlaceRecommendations(userId, 1L);
    }

    /**
     * 장소 타입에 따라 랜덤 장소 목록 조회
     * 최대 1개만 반환하도록 처리
     */
    private List<PlaceRecommendationResponseDto.PlaceDto> getRandomPlaces(Long sessionId, String type) {
        List<PlaceRecommendation> places = placeRepository.findBySessionIdAndType(sessionId, type);

        // 장소가 없는 경우 빈 목록 반환
        if (places.isEmpty()) {
            log.info("장소 추천 없음: sessionId={}, type={}", sessionId, type);
            return Collections.emptyList();
        }

        // 랜덤으로 1개 선택
        Random random = new Random();
        PlaceRecommendation randomPlace = places.get(random.nextInt(places.size()));

        log.info("장소 추천 선택: sessionId={}, type={}, selectedPlace={}",
                sessionId, type, randomPlace.getName());

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
                .latitude(place.getLatitude())
                .longitude(place.getLongitude())
                .build();
    }
}