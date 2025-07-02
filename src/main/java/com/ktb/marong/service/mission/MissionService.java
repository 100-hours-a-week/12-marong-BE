package com.ktb.marong.service.mission;

import com.ktb.marong.common.util.WeekCalculator;
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

        // 3. 오늘 이미 선택한 미션이 있는지 확인
        List<UserMission> todayMissions = userMissionRepository.findTodaysInProgressMissionsByUserAndGroup(
                userId, groupId, today, currentWeek);

        boolean canSelectToday = todayMissions.isEmpty();
        AvailableMissionResponseDto.MissionSelectionStatus todaySelection = null;

        if (!canSelectToday) {
            UserMission todayMission = todayMissions.get(0);
            Mission selectedMission = todayMission.getMission();
            todaySelection = AvailableMissionResponseDto.MissionSelectionStatus.builder()
                    .missionId(selectedMission.getId())
                    .title(selectedMission.getTitle())
                    .description(selectedMission.getDescription())
                    .difficulty(selectedMission.getDifficulty())
                    .selectedAt(todayMission.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .build();
        }

        // 4. 이번 주차에 이미 선택한 미션들 조회
        List<UserMission> weeklyMissions = userMissionRepository.findByUserIdAndGroupIdAndWeek(userId, groupId, currentWeek);
        List<Long> selectedMissionIds = weeklyMissions.stream()
                .filter(um -> "manual".equals(um.getSelectionType()))
                .map(um -> um.getMission().getId())
                .collect(Collectors.toList());

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

                    // 오늘 해당 미션을 선택한 사용자 수 조회
                    int currentSelections = countTodaySelectionsForMission(groupId, mission.getId(), today, currentWeek);
                    int maxSelections = groupMission.getMaxAssignable();
                    int remainingSelections = Math.max(0, maxSelections - currentSelections);
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

        Mission mission = missionRepository.findById(requestDto.getMissionId())
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_FOUND));

        if (!userGroupRepository.existsByUserIdAndGroupId(userId, requestDto.getGroupId())) {
            throw new CustomException(ErrorCode.GROUP_NOT_FOUND, "해당 그룹에 속하지 않은 사용자입니다.");
        }

        // 2. 마니또 매칭 확인
        int currentWeek = WeekCalculator.getCurrentWeek();
        if (manittoRepository.findByManittoIdAndGroupIdAndWeek(userId, requestDto.getGroupId(), currentWeek).isEmpty()) {
            throw new CustomException(ErrorCode.MANITTO_NOT_FOUND, "마니또 매칭이 되지 않아 미션을 선택할 수 없습니다.");
        }

        LocalDate today = LocalDate.now();

        // 3. 해당 미션이 현재 주차에 생성되어 있는지 확인
        if (!groupMissionRepository.existsByGroupIdAndMissionIdAndWeek(requestDto.getGroupId(), requestDto.getMissionId(), currentWeek)) {
            throw new CustomException(ErrorCode.MISSION_NOT_FOUND,
                    String.format("해당 미션(ID: %d)은 현재 주차(%d)에 생성되지 않았습니다.", requestDto.getMissionId(), currentWeek));
        }

        // 4. 오늘 이미 미션을 선택했는지 확인
        List<UserMission> todayMissions = userMissionRepository.findTodaysInProgressMissionsByUserAndGroup(
                userId, requestDto.getGroupId(), today, currentWeek);
        if (!todayMissions.isEmpty()) {
            throw new CustomException(ErrorCode.DAILY_MISSION_LIMIT_EXCEEDED, "오늘은 이미 미션을 선택했습니다.");
        }

        // 5. 이번 주차에 동일한 미션을 이미 선택했는지 확인
        List<UserMission> weeklyMissions = userMissionRepository.findByUserIdAndGroupIdAndWeek(
                userId, requestDto.getGroupId(), currentWeek);
        boolean alreadySelectedInWeek = weeklyMissions.stream()
                .filter(um -> "manual".equals(um.getSelectionType()))
                .anyMatch(um -> um.getMission().getId().equals(requestDto.getMissionId()));

        if (alreadySelectedInWeek) {
            throw new CustomException(ErrorCode.MISSION_ALREADY_COMPLETED, "이번 주차에 이미 선택한 미션입니다.");
        }

        // 6. GroupMission에서 설정된 선택 가능 인원 확인
        GroupMission groupMission = groupMissionRepository.findByGroupIdAndMissionIdAndWeek(
                        requestDto.getGroupId(), requestDto.getMissionId(), currentWeek)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_FOUND, "해당 그룹에서 생성되지 않은 미션입니다."));

        if (!groupMission.isSelectable()) {
            throw new CustomException(ErrorCode.DAILY_MISSION_LIMIT_EXCEEDED,
                    "해당 미션은 선택할 수 있는 인원이 마감되었습니다.");
        }

        // 7. 미션 선택 및 저장
        UserMission userMission = UserMission.builder()
                .user(user)
                .groupId(requestDto.getGroupId())
                .mission(mission)
                .week(currentWeek)
                .assignedDate(today)
                .selectionType("manual")
                .build();

        UserMission savedMission = userMissionRepository.save(userMission);

        // 8. GroupMission의 remaining_count 감소
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
     * 오늘 특정 미션을 선택한 사용자 수 조회 (그룹별)
     */
    private int countTodaySelectionsForMission(Long groupId, Long missionId, LocalDate date, Integer week) {
        List<UserMission> todaySelections = userMissionRepository.findTodaysInProgressMissionsByUserAndGroup(
                null, groupId, date, week); // userId를 null로 하여 모든 사용자 조회

        return (int) todaySelections.stream()
                .filter(um -> um.getMission().getId().equals(missionId))
                .filter(um -> "manual".equals(um.getSelectionType())) // 수동 선택만 카운트
                .count();
    }
}