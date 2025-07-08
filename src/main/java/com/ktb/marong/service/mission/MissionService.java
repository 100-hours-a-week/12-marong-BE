package com.ktb.marong.service.mission;

import com.ktb.marong.common.util.WeekCalculator;
import com.ktb.marong.config.EventConfig;
import com.ktb.marong.domain.group.Group;
import com.ktb.marong.domain.mission.GroupMission;
import com.ktb.marong.domain.mission.Mission;
import com.ktb.marong.domain.mission.UserMission;
import com.ktb.marong.domain.user.User;
import com.ktb.marong.dto.request.mission.SelectMissionRequestDto;
import com.ktb.marong.dto.response.mission.AvailableMissionResponseDto;
import com.ktb.marong.dto.response.mission.SelectMissionResponseDto;
import com.ktb.marong.exception.CustomException;
import com.ktb.marong.exception.ErrorCode;
import com.ktb.marong.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MissionService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final MissionRepository missionRepository;
    private final GroupMissionRepository groupMissionRepository;
    private final UserGroupRepository userGroupRepository;
    private final ManittoRepository manittoRepository;
    private final UserMissionRepository userMissionRepository;
    private final EventConfig eventConfig;

    private static final int MAX_DAILY_SELECTIONS_PER_MISSION = 5;

    /**
     * 선택 가능한 미션 목록 조회 (주차별, 그룹별 완전 분리)
     */
    @Transactional(readOnly = true)
    public AvailableMissionResponseDto getAvailableMissions(Long userId, Long groupId) {
        log.info("선택 가능한 미션 목록 조회: userId={}, groupId={}", userId, groupId);

        // 1. 기본 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        if (!userGroupRepository.existsByUserIdAndGroupId(userId, groupId)) {
            throw new CustomException(ErrorCode.GROUP_NOT_FOUND, "해당 그룹에 속하지 않은 사용자입니다.");
        }

        // 2. 마니또 매칭 확인
        int currentWeek = WeekCalculator.getCurrentWeek();
        if (manittoRepository.findByManittoIdAndGroupIdAndWeek(userId, groupId, currentWeek).isEmpty()) {
            throw new CustomException(ErrorCode.MANITTO_NOT_FOUND, "마니또 매칭이 되지 않아 미션을 선택할 수 없습니다.");
        }

        LocalDate today = LocalDate.now();

        // 3. 오늘 이미 선택한 미션들 조회 (이번 주차에서)
        List<UserMission> todayAssignedMissions = userMissionRepository.findAllMissionsAssignedOnDate(
                userId, groupId, today, currentWeek);

        List<Long> selectedMissionIds = todayAssignedMissions.stream()
                .map(um -> um.getMission().getId())
                .collect(Collectors.toList());

        // 4. 이벤트 기간에 따른 선택 가능 여부 확인
        boolean isEventPeriod = eventConfig.isUnlimitedMissionEventPeriod();
        boolean canSelectToday;
        String todaySelection;

        if (isEventPeriod) {
            // 이벤트 기간: 진행 중인 미션이 있으면 선택 불가
            List<UserMission> inProgressMissions = userMissionRepository.findTodaysInProgressMissionsByUserAndGroup(
                    userId, groupId, today, currentWeek);
            canSelectToday = inProgressMissions.isEmpty();
            todaySelection = inProgressMissions.isEmpty() ?
                    "이벤트 기간: 미션을 완료하면 바로 다음 미션 선택 가능" :
                    "진행 중인 미션을 완료해야 다음 미션 선택 가능";
        } else {
            // 일반 기간: 하루에 1개만 선택 가능
            canSelectToday = todayAssignedMissions.isEmpty();
            todaySelection = canSelectToday ? "오늘 미션을 선택하지 않음" :
                    String.format("오늘 선택한 미션: %d개 (하루 1개 제한)", todayAssignedMissions.size());
        }

        // 5. 해당 그룹의 현재 주차에 생성된 미션들만 조회
        List<GroupMission> availableGroupMissions = groupMissionRepository.findByGroupIdAndWeek(groupId, currentWeek);

        // 현재 주차에 미션을 생성하지 않은 경우 예외 처리
        if (availableGroupMissions.isEmpty()) {
            log.warn("현재 주차에 생성된 미션이 없음: groupId={}, week={}", groupId, currentWeek);
            throw new CustomException(ErrorCode.MISSION_NOT_FOUND,
                    String.format("현재 주차(%d)에 해당 그룹의 미션이 생성되지 않았습니다.", currentWeek));
        }

        List<AvailableMissionResponseDto.AvailableMissionDto> availableMissions = availableGroupMissions.stream()
                .map(groupMission -> {
                    Mission mission = groupMission.getMission();

                    // GroupMission의 remainingCount를 직접 사용 (매일 자정에 리셋됨)
                    int remainingSelections = groupMission.getRemainingCount();
                    int maxSelections = groupMission.getMaxAssignable();
                    int currentSelections = maxSelections - remainingSelections; // 현재까지 선택된 개수

                    boolean alreadySelectedInWeek = selectedMissionIds.contains(mission.getId());
                    boolean selectable = remainingSelections > 0 && !alreadySelectedInWeek && canSelectToday && groupMission.isSelectable();

                    return AvailableMissionResponseDto.AvailableMissionDto.builder()
                            .missionId(mission.getId())
                            .title(mission.getTitle())
                            .description(mission.getDescription())
                            .difficulty(mission.getDifficulty())
                            .currentSelections(currentSelections)
                            .maxSelections(maxSelections)
                            .remainingSelections(remainingSelections)
                            .alreadySelectedInWeek(alreadySelectedInWeek)
                            .selectable(selectable)
                            .build();
                })
                .collect(Collectors.toList());

        log.info("선택 가능한 미션 목록 조회 완료: userId={}, groupId={}, week={}, availableCount={}, canSelectToday={}",
                userId, groupId, currentWeek, availableMissions.size(), canSelectToday);

        return AvailableMissionResponseDto.builder()
                .groupId(groupId)
                .groupName(group.getName())
                .date(today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .canSelectToday(canSelectToday)
                .todaySelection(todaySelection)
                .availableMissions(availableMissions)
                .build();
    }

    /**
     * 미션 선택 (주차별, 그룹별 검증 강화)
     */
    @Transactional
    public SelectMissionResponseDto selectMission(Long userId, SelectMissionRequestDto requestDto) {
        log.info("미션 선택 요청: userId={}, missionId={}, groupId={}",
                userId, requestDto.getMissionId(), requestDto.getGroupId());

        // 1. 기본 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Group group = groupRepository.findById(requestDto.getGroupId())
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        if (!userGroupRepository.existsByUserIdAndGroupId(userId, requestDto.getGroupId())) {
            throw new CustomException(ErrorCode.GROUP_NOT_FOUND, "해당 그룹에 속하지 않은 사용자입니다.");
        }

        Mission mission = missionRepository.findById(requestDto.getMissionId())
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_FOUND));

        // 2. 마니또 매칭 확인
        int currentWeek = WeekCalculator.getCurrentWeek();
        if (manittoRepository.findByManittoIdAndGroupIdAndWeek(userId, requestDto.getGroupId(), currentWeek).isEmpty()) {
            throw new CustomException(ErrorCode.MANITTO_NOT_FOUND, "마니또 매칭이 되지 않아 미션을 선택할 수 없습니다.");
        }

        LocalDate today = LocalDate.now();

        // 3. 현재 주차에서 동일한 미션을 이미 선택했는지 확인
        List<UserMission> existingMissions = userMissionRepository.findByUserIdAndGroupIdAndWeek(userId, requestDto.getGroupId(), currentWeek);
        boolean alreadySelectedInWeek = existingMissions.stream()
                .anyMatch(um -> um.getMission().getId().equals(requestDto.getMissionId()));

        if (alreadySelectedInWeek) {
            throw new CustomException(ErrorCode.DAILY_MISSION_LIMIT_EXCEEDED,
                    "이번 주차에 이미 선택한 미션입니다.");
        }

        // 4. 이벤트 기간에 따른 선택 가능 여부 확인
        boolean isEventPeriod = eventConfig.isUnlimitedMissionEventPeriod();

        if (isEventPeriod) {
            // 이벤트 기간: 진행 중인 미션이 있으면 선택 불가
            List<UserMission> inProgressMissions = userMissionRepository.findTodaysInProgressMissionsByUserAndGroup(
                    userId, requestDto.getGroupId(), today, currentWeek);

            if (!inProgressMissions.isEmpty()) {
                throw new CustomException(ErrorCode.DAILY_MISSION_LIMIT_EXCEEDED,
                        "이벤트 기간 중 진행 중인 미션을 완료해야 다음 미션을 선택할 수 있습니다.");
            }
        } else {
            // 일반 기간: 하루에 1개만 선택 가능
            List<UserMission> todayAssignedMissions = userMissionRepository.findAllMissionsAssignedOnDate(
                    userId, requestDto.getGroupId(), today, currentWeek);

            if (!todayAssignedMissions.isEmpty()) {
                throw new CustomException(ErrorCode.DAILY_MISSION_LIMIT_EXCEEDED,
                        "하루에 하나의 미션만 선택할 수 있습니다.");
            }
        }

        // 5. GroupMission에서 설정된 선택 가능 인원 확인
        GroupMission groupMission = groupMissionRepository.findByGroupIdAndMissionIdAndWeek(
                        requestDto.getGroupId(), requestDto.getMissionId(), currentWeek)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_FOUND, "해당 그룹에서 생성되지 않은 미션입니다."));

        if (!groupMission.isSelectable()) {
            throw new CustomException(ErrorCode.DAILY_MISSION_LIMIT_EXCEEDED,
                    "해당 미션은 선택할 수 있는 인원이 마감되었습니다.");
        }

        // 6. 미션 선택 및 저장
        UserMission userMission = UserMission.builder()
                .user(user)
                .groupId(requestDto.getGroupId())
                .mission(mission)
                .week(currentWeek)
                .assignedDate(today)
                .selectionType("manual")
                .build();

        UserMission savedMission = userMissionRepository.save(userMission);

        // 7. GroupMission의 remaining_count 감소
        groupMission.decreaseRemainingCount();
        groupMissionRepository.save(groupMission);

        log.info("미션 선택 완료: userId={}, missionId={}, groupId={}, userMissionId={}, remainingCount={}",
                userId, requestDto.getMissionId(), requestDto.getGroupId(), savedMission.getId(), groupMission.getRemainingCount());

        return SelectMissionResponseDto.builder()
                .missionId(mission.getId())
                .title(mission.getTitle())
                .description(mission.getDescription())
                .difficulty(mission.getDifficulty())
                .groupId(requestDto.getGroupId())
                .selectedDate(today)
                .selectedAt(savedMission.getCreatedAt())
                .message("미션 선택이 완료되었습니다.")
                .build();
    }

    /**
     * 매일 자정에 모든 GroupMission의 remainingCount를 maxAssignable로 리셋
     * 매일 00:00:00에 실행
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void resetDailyMissionSelectionCounts() {
        log.info("매일 자정 미션 선택 가능 개수 리셋 시작");

        try {
            // 현재 주차 계산
            int currentWeek = WeekCalculator.getCurrentWeek();

            // 현재 주차에 있는 모든 GroupMission 조회
            List<GroupMission> currentWeekGroupMissions = groupMissionRepository.findByWeek(currentWeek);

            if (currentWeekGroupMissions.isEmpty()) {
                log.info("현재 주차({})에 리셋할 GroupMission이 없음", currentWeek);
                return;
            }

            // 각 GroupMission의 remainingCount를 maxAssignable로 리셋
            int resetCount = 0;
            for (GroupMission groupMission : currentWeekGroupMissions) {
                int beforeCount = groupMission.getRemainingCount();
                groupMission.resetDailyRemainingCount();
                int afterCount = groupMission.getRemainingCount();

                log.debug("GroupMission 리셋: groupId={}, missionId={}, week={}, before={}, after={}",
                        groupMission.getGroup().getId(),
                        groupMission.getMission().getId(),
                        groupMission.getWeek(),
                        beforeCount,
                        afterCount);

                resetCount++;
            }

            // 벌크 저장
            groupMissionRepository.saveAll(currentWeekGroupMissions);

            log.info("매일 자정 미션 선택 가능 개수 리셋 완료: 현재 주차={}, 리셋된 GroupMission 개수={}",
                    currentWeek, resetCount);

        } catch (Exception e) {
            log.error("매일 자정 미션 선택 가능 개수 리셋 중 오류 발생", e);
            // 트랜잭션 롤백됨
        }
    }
}