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
import org.springframework.scheduling.annotation.Scheduled;
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

        // 사용자(마니띠)의 마니또를 조회
        // findByGiverIdAndGroupIdAndWeek를 findByManitteeIdAndGroupIdAndWeek로 변경
        List<Manitto> manittoList = manittoRepository.findByManitteeIdAndGroupIdAndWeek(userId, 1L, currentWeek);

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

        // 여러 매칭 중 첫 번째 매칭 정보 사용
        Manitto manitto = manittoList.get(0);

        // 다음 공개까지 남은 시간 계산 (금요일 오후 5시 기준)
        String remainingTime = calculateRemainingTimeUntilReveal();

        return ManittoInfoResponseDto.builder()
                .manitto(ManittoInfoResponseDto.ManittoDto.builder()
                        .name(manitto.getManitto().getNickname())  // receiver에서 manitto로 변경
                        .profileImage(manitto.getManitto().getProfileImageUrl()) // receiver에서 manitto로 변경
                        .remainingTime(remainingTime)
                        .build())
                .build();
    }

    /**
     * 마니또 미션 상태 조회
     * 미션 수행 여부는 게시글 작성 여부로 판단
     */
    @Transactional
    public MissionStatusResponseDto getMissionStatus(Long userId) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 현재 주차에 해당하는 사용자 미션 조회
        int currentWeek = WeekCalculator.getCurrentWeek();
        List<UserMission> userMissions = userMissionRepository.findByUserIdAndGroupIdAndWeek(userId, 1L, currentWeek);

        // 진행 중인 미션, 완료된 미션, 미완료된 미션 구분
        List<UserMission> completedMissions = new ArrayList<>();
        List<UserMission> inProgressMissions = new ArrayList<>();
        List<UserMission> incompleteMissions = new ArrayList<>(); // 미완료 미션 리스트 추가

        for (UserMission userMission : userMissions) {
            // 미션 상태를 통해 먼저 판단
            if ("completed".equals(userMission.getStatus())) {
                completedMissions.add(userMission);
            } else if ("incomplete".equals(userMission.getStatus())) {
                incompleteMissions.add(userMission); // 미완료 미션 추가
            } else {
                // 게시글 작성 여부로 상태 추가 판단 (현재 주차의 해당 미션에 대한 게시글만 체크)
                int postCount = postRepository.countByUserIdAndMissionIdAndWeek(
                        userId, userMission.getMission().getId(), currentWeek);

                log.info("미션 완료 확인: userId={}, missionId={}, week={}, postCount={}",
                        userId, userMission.getMission().getId(), currentWeek, postCount);

                if (postCount > 0) {
                    // 게시글이 있으면 미션을 완료 상태로 업데이트하고 완료 목록에 추가
                    userMission.complete();
                    userMissionRepository.save(userMission);
                    completedMissions.add(userMission);
                } else {
                    inProgressMissions.add(userMission);
                }
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

        // 미완료 미션 DTO 변환
        List<MissionStatusResponseDto.MissionDto> incompleteMissionDtos =
                incompleteMissions.stream()
                        .map(this::convertToMissionDto)
                        .collect(Collectors.toList());

        // 진행률 계산
        int total = userMissions.size();
        int completed = completedMissions.size();
        int incomplete = incompleteMissions.size(); // 미완료 미션 개수

        return MissionStatusResponseDto.builder()
                .progress(MissionStatusResponseDto.ProgressDto.builder()
                        .completed(completed)
                        .incomplete(incomplete) // 미완료 미션 개수 추가
                        .total(total)
                        .build())
                .missions(MissionStatusResponseDto.MissionsDto.builder()
                        .inProgress(inProgressMissionDtos)
                        .completed(completedMissionDtos)
                        .incomplete(incompleteMissionDtos) // 미완료 미션 목록 추가
                        .build())
                .build();
    }

    /**
     * 새 미션 할당 - 하루 1개 제한 추가
     * 이전 날짜의 완료되지 않은 미션은 자동으로 '미완료' 상태로 변경
     */
    @Transactional
    public UserMission assignNewMission(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        int currentWeek = WeekCalculator.getCurrentWeek();
        LocalDate today = LocalDate.now();

        // 이전 날짜의 완료되지 않은 미션들을 '미완료' 상태로 변경
        handleExpiredMissions(userId, today);

        // 현재 주차에 해당하는 마니또 매칭 정보 조회 (그룹 ID 1 고정)
        // findByGiverIdAndGroupIdAndWeek를 findByManitteeIdAndGroupIdAndWeek로 변경
        List<Manitto> manittoList = manittoRepository.findByManitteeIdAndGroupIdAndWeek(userId, 1L, currentWeek);

        // 마니또 매칭 정보가 없는 경우 예외 발생 (추가된 부분)
        if (manittoList.isEmpty()) {
            throw new CustomException(ErrorCode.MANITTO_NOT_FOUND, "마니또 매칭 정보가 없어 미션을 할당할 수 없습니다.");
        }

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
     * 이전 날짜의 완료되지 않은 미션을 처리하는 메서드
     * 완료되지 않은 미션은 '미완료(incomplete)' 상태로 변경
     */
    private void handleExpiredMissions(Long userId, LocalDate today) {
        LocalDate yesterday = today.minusDays(1);

        // 현재 주차의 어제 이전에 할당된 미션 중 진행 중인 상태인 미션 조회
        List<UserMission> expiredMissions = userMissionRepository.findIncompleteMissionsBeforeDate(
                userId, yesterday);

        if (!expiredMissions.isEmpty()) {
            for (UserMission mission : expiredMissions) {
                // 미완료 상태로 변경
                mission.markAsIncomplete();
                userMissionRepository.save(mission);
                log.info("미완료 미션 처리: userId={}, missionId={}, assignedDate={}",
                        userId, mission.getMission().getId(), mission.getAssignedDate());
            }
        }
    }

    /**
     * 마니또 주기 변경에 따른 미션 초기화 (주차 변경 시 자동 실행)
     * 매주 월요일 오전 9시에 실행
     */
    @Scheduled(cron = "0 0 9 * * MON")
    @Transactional
    public void resetMissionsForNewCycle() {
        int currentWeek = WeekCalculator.getCurrentWeek();
        int previousWeek = currentWeek - 1;
        log.info("새로운 마니또 주기 시작: 현재 주차={}, 이전 주차={}", currentWeek, previousWeek);

        // 이전 주차의 진행 중인 미션을 미완료 처리
        List<UserMission> inProgressMissions = userMissionRepository.findByStatusAndWeek("ing", previousWeek);
        for (UserMission mission : inProgressMissions) {
            mission.markAsIncomplete();
            userMissionRepository.save(mission);
            log.info("이전 주차 미완료 미션 처리: userId={}, missionId={}, week={}",
                    mission.getUser().getId(), mission.getMission().getId(), previousWeek);
        }

        // 중요: 현재 주차에 잘못 생성된 미션이 있으면 삭제
        // 이는 테스트 데이터나 다른 로직에 의해 생성된 미션일 수 있음
        List<UserMission> currentWeekMissions = userMissionRepository.findByWeek(currentWeek);
        if (!currentWeekMissions.isEmpty()) {
            for (UserMission mission : currentWeekMissions) {
                userMissionRepository.delete(mission);
                log.info("현재 주차 미션 초기화: userId={}, missionId={}, week={}",
                        mission.getUser().getId(), mission.getMission().getId(), currentWeek);
            }
        }

        // 새로운 주차 시작 로그
        log.info("주차 {} 시작: 모든 사용자의 미션이 초기화되었습니다.", currentWeek);
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
                .missionId(userMission.getMission().getId()) // 미션 ID 추가
                .title(userMission.getMission().getTitle())
                .description(userMission.getMission().getDescription())
                .difficulty(userMission.getMission().getDifficulty())
                .build();
    }

    /**
     * 다음 마니또 공개(월요일 오후 12시)까지 남은 시간 계산
     * 형식: HH:MM:SS
     */
    private String calculateRemainingTimeUntilReveal() {
        LocalDateTime now = LocalDateTime.now();

        // 이번 주 월요일 오후 12시 계산
        LocalDateTime targetTime = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
                .with(LocalTime.of(12, 0, 0));

        // 이미 지났으면 다음 주 월요일로 설정
        if (now.isAfter(targetTime)) {
            targetTime = now.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                    .with(LocalTime.of(12, 0, 0));
        }

        // 남은 시간 계산
        long seconds = ChronoUnit.SECONDS.between(now, targetTime);

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds);
    }
}