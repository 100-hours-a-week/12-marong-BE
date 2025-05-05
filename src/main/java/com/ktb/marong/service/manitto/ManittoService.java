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
        Manitto manitto = manittoRepository.findByGiverIdAndGroupIdAndWeek(userId, 1L, currentWeek)
                .orElseThrow(() -> new CustomException(ErrorCode.MANITTO_NOT_FOUND, "현재 매칭된 마니또가 없습니다."));

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

            if (postCount > 0) {
                completedMissions.add(userMission);
                // 미션 상태 업데이트 (별도 트랜잭션 필요)
                if ("ing".equals(userMission.getStatus())) {
                    updateMissionStatus(userMission.getId());
                }
            } else {
                inProgressMissions.add(userMission);
            }
        }

        // 진행 중인 미션이 없고 완료된 미션이 있으면 새 미션 할당 (하루 1개)
        if (inProgressMissions.isEmpty() && userMissions.size() < 5) { // 최대 5개 (일주일 중 영업일인 5일)
            Mission newMission = getAvailableMission(userMissions);
            if (newMission != null) {
                UserMission userMission = assignMissionToUser(user, newMission, currentWeek);
                inProgressMissions.add(userMission);
            }
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
     * 미션 완료 상태 업데이트 (별도 트랜잭션)
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
     * 사용자에게 미션 할당
     */
    @Transactional
    public UserMission assignMissionToUser(User user, Mission mission, int week) {
        UserMission userMission = UserMission.builder()
                .user(user)
                .groupId(1L) // MVP에서는 기본 그룹 ID 1로 고정
                .mission(mission)
                .week(week)
                .build();

        return userMissionRepository.save(userMission);
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