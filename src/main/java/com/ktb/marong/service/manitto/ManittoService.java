package com.ktb.marong.service.manitto;

import com.ktb.marong.common.util.WeekCalculator;
import com.ktb.marong.domain.manitto.Manitto;
import com.ktb.marong.domain.mission.Mission;
import com.ktb.marong.domain.mission.UserMission;
import com.ktb.marong.domain.user.User;
import com.ktb.marong.dto.response.manitto.ManittoInfoResponseDto;
import com.ktb.marong.dto.response.manitto.MissionStatusResponseDto;
import com.ktb.marong.exception.CustomException;
import com.ktb.marong.exception.ErrorCode;
import com.ktb.marong.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManittoService {

    private final UserRepository userRepository;
    private final MissionRepository missionRepository;
    private final UserMissionRepository userMissionRepository;
    private final ManittoRepository manittoRepository;
    private final PostRepository postRepository;

    /**
     * 현재 마니또 정보 조회
     * AI가 알려준 마니또 매칭 정보를 DB에서 조회
     */
    @Transactional(readOnly = true)
    public ManittoInfoResponseDto getCurrentManittoInfo(Long userId) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 현재 주차에 해당하는 마니또 매칭 정보 조회
        int currentWeek = WeekCalculator.getCurrentWeek();

        // findByGiverIdAndGroupIdAndWeek가 리스트를 반환하므로 첫 번째 요소를 가져옴
        List<Manitto> manittoList = manittoRepository.findByGiverIdAndGroupIdAndWeek(userId, 1L, currentWeek);

        // 매칭 정보가 없을 경우 기본 응답
        if (manittoList.isEmpty()) {
            return ManittoInfoResponseDto.builder()
                    .manitto(ManittoInfoResponseDto.ManittoDto.builder()
                            .name("아직 매칭된 마니또가 없습니다")
                            .profileImage(null)
                            .remainingTime(calculateRemainingTimeUntilReveal())
                            .build())
                    .build();
        }

        // 여러 매칭 중 첫 번째 매칭 정보 사용 (또는 다른 로직으로 선택)
        Manitto manitto = manittoList.get(0);

        // 다음 공개까지 남은 시간 계산 (금요일 오후 5시 기준)
        String remainingTime = calculateRemainingTimeUntilReveal();

        return ManittoInfoResponseDto.builder()
                .manitto(ManittoInfoResponseDto.ManittoDto.builder()
                        .name(manitto.getReceiver().getNickname())
                        .profileImage(manitto.getReceiver().getProfileImageUrl())
                        .remainingTime(remainingTime)
                        .build())
                .build();
    }

    /**
     * 마니또 미션 상태 조회
     * 미션 수행 여부는 게시글 작성 여부로 판단
     */
    @Transactional(readOnly = true)
    public MissionStatusResponseDto getMissionStatus(Long userId) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 현재 주차에 해당하는 사용자 미션 조회
        int currentWeek = WeekCalculator.getCurrentWeek();
        List<UserMission> userMissions = userMissionRepository.findByUserIdAndGroupIdAndWeek(userId, 1L, currentWeek);

        // 진행 중인 미션과 완료된 미션 구분
        List<UserMission> completedMissions = new ArrayList<>();
        List<UserMission> inProgressMissions = new ArrayList<>();

        for (UserMission userMission : userMissions) {
            // 미션 수행 여부는 게시글 작성 여부로 판단
            int postCount = postRepository.countByUserIdAndMissionId(userId, userMission.getMission().getId());

            if (postCount > 0 || "completed".equals(userMission.getStatus())) {
                completedMissions.add(userMission);
            } else {
                inProgressMissions.add(userMission);
            }
        }

        // 진행 중인 미션이 없고 전체 미션이 5개 미만이면 새 미션 할당이 필요하다는 로그만 남김
        if (inProgressMissions.isEmpty() && userMissions.size() < 5) {
            log.info("새로운 미션 할당이 필요합니다. 사용자 ID: {}", userId);
            // 여기서는 미션을 생성하지 않고, 필요하다는 정보만 로깅
        }

        // DTO 변환
        List<MissionStatusResponseDto.MissionDto> inProgressMissionDtos =
                inProgressMissions.stream()
                        .map(this::convertToMissionDto)
                        .collect(Collectors.toList());

        List<MissionStatusResponseDto.MissionDto> completedMissionDtos =
                completedMissions.stream()
                        .map(this::convertToMissionDto)
                        .collect(Collectors.toList());

        // 진행률 계산
        int total = inProgressMissions.size() + completedMissions.size();
        int completed = completedMissions.size();

        return MissionStatusResponseDto.builder()
                .progress(MissionStatusResponseDto.ProgressDto.builder()
                        .completed(completed)
                        .total(total)
                        .build())
                .missions(MissionStatusResponseDto.MissionsDto.builder()
                        .inProgress(inProgressMissionDtos)
                        .completed(completedMissionDtos)
                        .build())
                .build();
    }

    /**
     * 새 미션 할당 - 하루 1개 제한 추가
     */
    @Transactional
    public UserMission assignNewMission(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        int currentWeek = WeekCalculator.getCurrentWeek();
        LocalDate today = LocalDate.now();

        // 오늘 이미 할당된 미션이 있는지 확인
        boolean hasTodayMission = userMissionRepository.existsByUserIdAndAssignedDate(userId, today);
        if (hasTodayMission) {
            throw new CustomException(ErrorCode.DAILY_MISSION_LIMIT_EXCEEDED, "하루에 한 개의 미션만 수행할 수 있습니다.");
        }

        // 현재 주차에 해당하는 사용자 미션 조회 (미션 중복 방지용)
        List<UserMission> userMissions = userMissionRepository.findByUserIdAndGroupIdAndWeek(userId, 1L, currentWeek);

        // 할당 가능한 미션 찾기
        Mission mission = getAvailableMission(userMissions);
        if (mission == null) {
            throw new CustomException(ErrorCode.MISSION_NOT_FOUND, "할당 가능한 미션이 없습니다.");
        }

        // 미션 생성 및 저장
        UserMission userMission = UserMission.builder()
                .user(user)
                .groupId(1L)
                .mission(mission)
                .week(currentWeek)
                .assignedDate(today) // 오늘 날짜로 할당
                .build();

        return userMissionRepository.save(userMission);
    }

    /**
     * 미션 완료 상태 업데이트
     */
    @Transactional
    public void updateMissionStatus(Long userMissionId) {
        UserMission userMission = userMissionRepository.findById(userMissionId)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_FOUND));
        userMission.complete();
        userMissionRepository.save(userMission);
    }

    /**
     * 사용자에게 아직 할당되지 않은 미션 찾기
     */
    private Mission getAvailableMission(List<UserMission> existingMissions) {
        // 이미 할당된 미션의 ID 목록
        List<Long> assignedMissionIds = existingMissions.stream()
                .map(um -> um.getMission().getId())
                .collect(Collectors.toList());

        // 전체 미션 중 아직 할당되지 않은 미션 찾기
        List<Mission> allMissions = missionRepository.findAll();
        List<Mission> availableMissions = allMissions.stream()
                .filter(mission -> !assignedMissionIds.contains(mission.getId()))
                .collect(Collectors.toList());

        if (availableMissions.isEmpty()) {
            return null;
        }

        // 랜덤으로 하나 선택
        int randomIndex = (int) (Math.random() * availableMissions.size());
        return availableMissions.get(randomIndex);
    }

    /**
     * UserMission을 MissionDto로 변환
     */
    private MissionStatusResponseDto.MissionDto convertToMissionDto(UserMission userMission) {
        return MissionStatusResponseDto.MissionDto.builder()
                .title(userMission.getMission().getTitle())
                .description(userMission.getMission().getDescription())
                .build();
    }

    /**
     * 다음 마니또 공개(금요일 오후 5시)까지 남은 시간 계산
     * 형식: HH:MM:SS
     */
    private String calculateRemainingTimeUntilReveal() {
        LocalDateTime now = LocalDateTime.now();

        // 이번 주 금요일 오후 5시 계산
        LocalDateTime targetTime = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY))
                .with(LocalTime.of(17, 0, 0));

        // 이미 지났으면 다음 주 금요일로 설정
        if (now.isAfter(targetTime)) {
            targetTime = now.with(TemporalAdjusters.next(DayOfWeek.FRIDAY))
                    .with(LocalTime.of(17, 0, 0));
        }

        // 남은 시간 계산
        long seconds = ChronoUnit.SECONDS.between(now, targetTime);

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds);
    }
}