package com.ktb.marong.service.recommendation;

import com.ktb.marong.common.util.WeekCalculator;
import com.ktb.marong.domain.group.Group;
import com.ktb.marong.domain.manitto.Manitto;
import com.ktb.marong.domain.recommendation.PlaceLike;
import com.ktb.marong.domain.recommendation.PlaceRecommendation;
import com.ktb.marong.domain.recommendation.PlaceRecommendationSession;
import com.ktb.marong.domain.user.User;
import com.ktb.marong.dto.request.recommendation.PlaceLikeRequestDto;
import com.ktb.marong.dto.response.recommendation.PlaceLikeResponseDto;
import com.ktb.marong.dto.response.recommendation.PlaceRecommendationResponseDto;
import com.ktb.marong.exception.CustomException;
import com.ktb.marong.exception.ErrorCode;
import com.ktb.marong.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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
    private final PlaceLikeRepository placeLikeRepository;

    /**
     * 장소 추천 조회 (밥집 & 카페) - 그룹별 분리
     */
    @Transactional(readOnly = true)
    public PlaceRecommendationResponseDto getPlaceRecommendations(Long userId, Long groupId) {
        log.info("장소 추천 조회 시작: userId={}, groupId={}", userId, groupId);

        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 그룹 존재 여부 확인
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
                    "해당 그룹에서 마니또 매칭 정보가 없어 장소 추천을 제공할 수 없습니다.");
        }

        // 6. 첫 번째 마니또 정보 사용 (일반적으로 하나만 존재)
        Manitto manitto = manittoList.get(0);
        log.info("마니또 매칭 정보: userId={}, manittoUserId={}, groupId={}, week={}",
                userId, manitto.getManittee().getId(), groupId, currentWeek);

        // 7. 해당 마니또 조합의 추천 세션 조회
        List<PlaceRecommendationSession> sessions = sessionRepository
                .findByManittoIdAndManitteeIdAndWeek(userId, manitto.getManittee().getId(), currentWeek);

        if (sessions.isEmpty()) {
            log.info("추천 세션 없어 빈 추천 목록 반환: userId={}, manittoUserId={}, groupId={}, week={}",
                    userId, manitto.getManittee().getId(), groupId, currentWeek);
            return PlaceRecommendationResponseDto.builder()
                    .restaurants(Collections.emptyList())
                    .cafes(Collections.emptyList())
                    .build();
        }

        PlaceRecommendationSession session = sessions.get(0);
        log.info("추천 세션 조회 완료: sessionId={}, userId={}, manittoUserId={}, groupId={}, week={}",
                session.getId(), userId, manitto.getManittee().getId(), groupId, currentWeek);

        // 8. 레스토랑 목록 조회 및 DTO 변환
        List<PlaceRecommendationResponseDto.PlaceDto> restaurants = getRandomPlacesWithLikes(session.getId(), "restaurant", userId);

        // 9. 카페 목록 조회 및 DTO 변환
        List<PlaceRecommendationResponseDto.PlaceDto> cafes = getRandomPlacesWithLikes(session.getId(), "cafe", userId);

        log.info("장소 추천 조회 완료: userId={}, groupId={}, sessionId={}, restaurants={}, cafes={}",
                userId, groupId, session.getId(), restaurants.size(), cafes.size());

        // 10. 응답 생성
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
     * 장소 추천 좋아요 토글 (등록/취소)
     */
    @Transactional
    public PlaceLikeResponseDto.ToggleResponse togglePlaceLike(Long userId, Long placeRecommendationId, PlaceLikeRequestDto requestDto) {
        log.info("장소 추천 좋아요 토글 시작: userId={}, placeRecommendationId={}, cancel={}",
                userId, placeRecommendationId, requestDto.getCancel());

        // 1. 사용자 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        // 2. 장소 추천 존재 확인
        PlaceRecommendation placeRecommendation = placeRepository.findById(placeRecommendationId)
                .orElseThrow(() -> new CustomException(ErrorCode.RECOMMENDATION_NOT_FOUND, "장소 추천을 찾을 수 없습니다."));

        boolean isCancel = requestDto.getCancel();
        Optional<PlaceLike> existingLike = placeLikeRepository.findByUserIdAndPlaceRecommendationId(userId, placeRecommendationId);

        if (isCancel) {
            // 좋아요 취소
            PlaceLike placeLike = existingLike
                    .orElseThrow(() -> new CustomException(ErrorCode.NOT_LIKED, "좋아요를 찾을 수 없습니다."));

            placeLikeRepository.delete(placeLike);
            log.info("장소 추천 좋아요 취소 완료: likeId={}", placeLike.getId());

            // 총 좋아요 수 조회 (삭제 후)
            long totalLikes = placeLikeRepository.countByPlaceRecommendationId(placeRecommendationId);

            return PlaceLikeResponseDto.ToggleResponse.builder()
                    .userId(userId)
                    .placeRecommendationId(placeRecommendationId)
                    .placeName(placeRecommendation.getName())
                    .placeType(placeRecommendation.getType())
                    .liked(false)
                    .totalLikes(totalLikes)
                    .likedAt(null)
                    .build();
        } else {
            // 좋아요 등록
            if (existingLike.isPresent()) {
                throw new CustomException(ErrorCode.ALREADY_LIKED, "이미 좋아요를 누른 장소입니다.");
            }

            PlaceLike placeLike = PlaceLike.builder()
                    .user(user)
                    .placeRecommendation(placeRecommendation)
                    .build();

            PlaceLike savedLike = placeLikeRepository.save(placeLike);
            log.info("장소 추천 좋아요 등록 완료: likeId={}", savedLike.getId());

            // 총 좋아요 수 조회
            long totalLikes = placeLikeRepository.countByPlaceRecommendationId(placeRecommendationId);

            return PlaceLikeResponseDto.ToggleResponse.builder()
                    .userId(userId)
                    .placeRecommendationId(placeRecommendationId)
                    .placeName(placeRecommendation.getName())
                    .placeType(placeRecommendation.getType())
                    .liked(true)
                    .totalLikes(totalLikes)
                    .likedAt(savedLike.getCreatedAt())
                    .build();
        }
    }

    /**
     * 사용자가 좋아요한 모든 장소 조회
     */
    @Transactional(readOnly = true)
    public PlaceLikeResponseDto.UserLikes getUserLikedPlaces(Long userId) {
        log.info("사용자 좋아요 장소 조회: userId={}", userId);

        // 1. 사용자 존재 확인
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }

        // 2. 사용자의 모든 좋아요 조회
        List<PlaceLike> userLikes = placeLikeRepository.findByUserId(userId);

        // 3. DTO 변환
        List<PlaceLikeResponseDto.UserLikes.LikedPlace> likedPlaces = userLikes.stream()
                .map(like -> PlaceLikeResponseDto.UserLikes.LikedPlace.builder()
                        .placeRecommendationId(like.getPlaceRecommendation().getId())
                        .placeName(like.getPlaceRecommendation().getName())
                        .placeType(like.getPlaceRecommendation().getType())
                        .category(like.getPlaceRecommendation().getCategory())
                        .address(like.getPlaceRecommendation().getAddress())
                        .likedAt(like.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return PlaceLikeResponseDto.UserLikes.builder()
                .userId(userId)
                .totalLikedPlaces(likedPlaces.size())
                .likedPlaces(likedPlaces)
                .build();
    }

    /**
     * 장소 추천의 좋아요 상태 확인
     */
    @Transactional(readOnly = true)
    public boolean isPlaceLikedByUser(Long userId, Long placeRecommendationId) {
        return placeLikeRepository.existsByUserIdAndPlaceRecommendationId(userId, placeRecommendationId);
    }

    /**
     * 장소 타입에 따라 랜덤 장소 목록 조회
     * 최대 1개만 반환하도록 처리
     */
    private List<PlaceRecommendationResponseDto.PlaceDto> getRandomPlacesWithLikes(Long sessionId, String type, Long userId) {
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
        PlaceRecommendationResponseDto.PlaceDto placeDto = convertToPlaceDtoWithLikes(randomPlace, userId);

        // 단일 아이템 리스트로 반환
        return Collections.singletonList(placeDto);
    }

    /**
     * 장소 엔티티를 DTO로 변환
     */
    private PlaceRecommendationResponseDto.PlaceDto convertToPlaceDtoWithLikes(PlaceRecommendation place, Long userId) {
        // 현재 사용자의 좋아요 여부 확인
        boolean isLiked = placeLikeRepository.existsByUserIdAndPlaceRecommendationId(userId, place.getId());

        // 총 좋아요 수 조회
        long totalLikes = placeLikeRepository.countByPlaceRecommendationId(place.getId());

        return PlaceRecommendationResponseDto.PlaceDto.builder()
                .placeId(place.getId()) // 좋아요 기능을 위한 ID 추가
                .name(place.getName())
                .category(place.getCategory())
                .hours(place.getOpeningHours())
                .address(place.getAddress())
                .latitude(place.getLatitude())
                .longitude(place.getLongitude())
                .liked(isLiked)
                .totalLikes(totalLikes)
                .build();
    }
}