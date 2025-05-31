package com.ktb.marong.service.manitto;

import com.ktb.marong.common.util.WeekCalculator;
import com.ktb.marong.domain.group.Group;
import com.ktb.marong.domain.group.UserGroup;
import com.ktb.marong.domain.manitto.Manitto;
import com.ktb.marong.domain.mission.Mission;
import com.ktb.marong.domain.mission.UserMission;
import com.ktb.marong.domain.user.User;
import com.ktb.marong.dto.response.manitto.ManittoDetailResponseDto;
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
    private final AnonymousNameRepository anonymousNameRepository;
    private final UserGroupRepository userGroupRepository;

    /**
     * 현재 사용자의 마니또-마니띠 상세 정보 조회 (그룹별, 시간대별)
     */
    @Transactional(readOnly = true)
    public ManittoDetailResponseDto getCurrentManittoDetail(Long userId, Long groupId) {
        log.info("마니또 상세 정보 조회 시작: userId={}, groupId={}", userId, groupId);

        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 사용자가 해당 그룹에 속해있는지 확인
        UserGroup userGroup = userGroupRepository.findByUserIdAndGroupId(userId, groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND, "해당 그룹에 속하지 않은 사용자입니다."));

        Group group = userGroup.getGroup();

        // 3. 현재 주차 및 시간 정보
        int currentWeek = WeekCalculator.getCurrentWeek();
        String remainingTime = calculateRemainingTimeUntilReveal(); // 기존 메서드 사용
        String currentPeriod = getCurrentPeriod();

        log.info("현재 시간 정보: currentWeek={}, period={}, remainingTime={}",
                currentWeek, currentPeriod, remainingTime);

        // 4. 시간대에 따른 분기 처리
        if ("MANITTO_REVEAL".equals(currentPeriod)) {
            // 마니또 공개 기간 (금요일 17시 ~ 월요일 12시)
            return buildRevealPeriodResponse(userId, groupId, group, currentWeek, remainingTime);
        } else {
            // 일반 활동 기간 (월요일 12시 ~ 금요일 17시)
            return buildActivePeriodResponse(userId, groupId, group, currentWeek, remainingTime);
        }
    }

    /**
     * 마니또 공개 기간 응답 생성 (금요일 17시 ~ 월요일 12시) -> MANITTO_REVEAL
     */
    private ManittoDetailResponseDto buildRevealPeriodResponse(Long userId, Long groupId, Group group,
                                                               int currentWeek, String remainingTime) {
        log.info("마니또 공개 기간 응답 생성: userId={}, groupId={}, week={}", userId, groupId, currentWeek);

        // 나를 담당했던 마니또 정보 조회 (이번 주기)
        List<Manitto> manitteeList = manittoRepository.findByManitteeIdAndGroupIdAndWeek(userId, groupId, currentWeek);

        ManittoDetailResponseDto.RevealedManittoDto revealedManitto = null;

        if (!manitteeList.isEmpty()) {
            Manitto manittoRelation = manitteeList.get(0);
            User manittoUser = manittoRelation.getManitto();

            // 마니또의 그룹 내 정보 조회
            UserGroup manittoUserGroup = userGroupRepository.findByUserIdAndGroupId(manittoUser.getId(), groupId)
                    .orElse(null);

            // 마니또의 이번 주기 익명 이름 조회
            String manittoAnonymousName = anonymousNameRepository
                    .findAnonymousNameByUserIdAndGroupIdAndWeek(manittoUser.getId(), groupId, currentWeek)
                    .orElse("익명의 마니또");

            revealedManitto = ManittoDetailResponseDto.RevealedManittoDto.builder()
                    .name(manittoUser.getNickname()) // 카카오 실명
                    .groupNickname(manittoUserGroup != null ? manittoUserGroup.getGroupUserNickname() : null)
                    .groupProfileImage(manittoUserGroup != null ? manittoUserGroup.getGroupUserProfileImageUrl() : null)
                    .anonymousName(manittoAnonymousName)
                    .build();

            log.info("공개된 마니또 정보: manittoUserId={}, name={}, groupNickname={}, anonymousName={}",
                    manittoUser.getId(), manittoUser.getNickname(),
                    manittoUserGroup != null ? manittoUserGroup.getGroupUserNickname() : "없음",
                    manittoAnonymousName);
        }

        return ManittoDetailResponseDto.builder()
                .period("MANITTO_REVEAL")
                .remainingTime(remainingTime)
                .groupId(groupId)
                .groupName(group.getName())
                .revealedManitto(revealedManitto)
                .build();
    }

    /**
     * 일반 활동 기간 응답 생성 (월요일 12시 ~ 금요일 17시) -> MANITTO_ACTIVE
     */
    private ManittoDetailResponseDto buildActivePeriodResponse(Long userId, Long groupId, Group group,
                                                               int currentWeek, String remainingTime) {
        log.info("일반 활동 기간 응답 생성: userId={}, groupId={}, week={}", userId, groupId, currentWeek);

        // 이전 주기 마니또 정보 (새로운 주기 첫 주에만 표시)
        ManittoDetailResponseDto.PreviousCycleManittoDto previousCycleManitto = null;
        int previousWeek = currentWeek - 1;

        if (previousWeek > 0) { // 1주차가 아닌 경우에만 이전 주기 정보 조회
            List<Manitto> previousManitteeList = manittoRepository.findByManitteeIdAndGroupIdAndWeek(userId, groupId, previousWeek);

            if (!previousManitteeList.isEmpty()) {
                Manitto previousManittoRelation = previousManitteeList.get(0);
                User previousManittoUser = previousManittoRelation.getManitto();

                // 이전 마니또의 그룹 내 정보 조회
                UserGroup previousManittoUserGroup = userGroupRepository.findByUserIdAndGroupId(previousManittoUser.getId(), groupId)
                        .orElse(null);

                // 이전 마니또의 익명 이름 조회
                String previousManittoAnonymousName = anonymousNameRepository
                        .findAnonymousNameByUserIdAndGroupIdAndWeek(previousManittoUser.getId(), groupId, previousWeek)
                        .orElse("익명의 마니또");

                previousCycleManitto = ManittoDetailResponseDto.PreviousCycleManittoDto.builder()
                        .name(previousManittoUser.getNickname())
                        .groupNickname(previousManittoUserGroup != null ? previousManittoUserGroup.getGroupUserNickname() : null)
                        .groupProfileImage(previousManittoUserGroup != null ? previousManittoUserGroup.getGroupUserProfileImageUrl() : null)
                        .anonymousName(previousManittoAnonymousName)
                        .build();

                log.info("이전 주기 마니또 정보: userId={}, name={}, anonymousName={}",
                        previousManittoUser.getId(), previousManittoUser.getNickname(), previousManittoAnonymousName);
            }
        }

        // 현재 나를 담당하는 마니또 정보 (익명)
        ManittoDetailResponseDto.CurrentManittoDto currentManitto = null;
        List<Manitto> currentManitteeList = manittoRepository.findByManitteeIdAndGroupIdAndWeek(userId, groupId, currentWeek);

        if (!currentManitteeList.isEmpty()) {
            Manitto currentManittoRelation = currentManitteeList.get(0);
            User currentManittoUser = currentManittoRelation.getManitto();

            String currentManittoAnonymousName = anonymousNameRepository
                    .findAnonymousNameByUserIdAndGroupIdAndWeek(currentManittoUser.getId(), groupId, currentWeek)
                    .orElse("익명의 마니또");

            currentManitto = ManittoDetailResponseDto.CurrentManittoDto.builder()
                    .anonymousName(currentManittoAnonymousName)
                    .build();

            log.info("현재 마니또 정보: manittoUserId={}, anonymousName={}",
                    currentManittoUser.getId(), currentManittoAnonymousName);
        }

        // 현재 내가 담당하는 마니띠 정보
        ManittoDetailResponseDto.CurrentManitteeDto currentManittee = null;
        List<Manitto> currentManittoList = manittoRepository.findByManittoIdAndGroupIdAndWeek(userId, groupId, currentWeek);

        if (!currentManittoList.isEmpty()) {
            Manitto currentManitteeRelation = currentManittoList.get(0);
            User currentManitteeUser = currentManitteeRelation.getManittee();

            // 마니띠의 그룹 내 정보 조회
            UserGroup manitteeUserGroup = userGroupRepository.findByUserIdAndGroupId(currentManitteeUser.getId(), groupId)
                    .orElse(null);

            // 그룹 닉네임이 없으면 카카오 이름 사용
            String displayName = (manitteeUserGroup != null && manitteeUserGroup.getGroupUserNickname() != null)
                    ? manitteeUserGroup.getGroupUserNickname()
                    : currentManitteeUser.getNickname();

            currentManittee = ManittoDetailResponseDto.CurrentManitteeDto.builder()
                    .name(currentManitteeUser.getNickname()) // 카카오 실명
                    .groupNickname(displayName) // 그룹 닉네임 우선, 없으면 카카오 이름
                    .groupProfileImage(manitteeUserGroup != null ? manitteeUserGroup.getGroupUserProfileImageUrl() : null)
                    .build();

            log.info("현재 마니띠 정보: manitteeUserId={}, name={}, groupNickname={}",
                    currentManitteeUser.getId(), currentManitteeUser.getNickname(), displayName);
        }

        return ManittoDetailResponseDto.builder()
                .period("MANITTO_ACTIVE")
                .remainingTime(remainingTime)
                .groupId(groupId)
                .groupName(group.getName())
                .previousCycleManitto(previousCycleManitto)
                .currentManitto(currentManitto)
                .currentManittee(currentManittee)
                .build();
    }

    /**
     * 현재 시간이 어떤 기간에 속하는지 판단
     * @return "MANITTO_REVEAL" 또는 "MANITTO_ACTIVE"
     */
    private String getCurrentPeriod() {
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek dayOfWeek = now.getDayOfWeek();
        int hour = now.getHour();

        // 금요일 17시 이후인 경우
        if (dayOfWeek == DayOfWeek.FRIDAY && hour >= 17) {
            return "MANITTO_REVEAL";
        }
        // 토요일, 일요일인 경우
        else if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return "MANITTO_REVEAL";
        }
        // 월요일 12시 이전인 경우
        else if (dayOfWeek == DayOfWeek.MONDAY && hour < 12) {
            return "MANITTO_REVEAL";
        }
        // 그 외의 경우 (월요일 12시 ~ 금요일 17시)
        else {
            return "MANITTO_ACTIVE";
        }
    }

    /**
     * 현재 사용자의 마니또/마니띠 역할 및 정보 조회 (그룹 ID 파라미터 추가)
     * @deprecated -> MVP 이후는 getCurrentManittoDetail 메소드 사용
     */
    @Deprecated
    @Transactional(readOnly = true)
    public ManittoInfoResponseDto getCurrentManittoInfo(Long userId, Long groupId) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 사용자가 해당 그룹에 속해있는지 확인
        if (!userGroupRepository.existsByUserIdAndGroupId(userId, groupId)) {
            throw new CustomException(ErrorCode.GROUP_NOT_FOUND, "해당 그룹에 속하지 않은 사용자입니다.");
        }

        // 현재 주차에 해당하는 매칭 정보 조회
        int currentWeek = WeekCalculator.getCurrentWeek();
        String remainingTime = calculateRemainingTimeUntilReveal();

        // 1. 사용자가 마니또인지 확인 (manitto_id로 매칭된 레코드가 있는지 확인)
        List<Manitto> isManittoList = manittoRepository.findByManittoIdAndGroupIdAndWeek(userId, groupId, currentWeek);

        if (!isManittoList.isEmpty()) {
            // 사용자가 마니또인 경우 - 담당하는 마니띠 정보 반환
            Manitto manitto = isManittoList.get(0);

            return ManittoInfoResponseDto.builder()
                    .role("manitto")
                    .remainingTime(remainingTime)
                    .manitteeName(manitto.getManittee().getNickname())
                    .manitteeProfileImage(manitto.getManittee().getProfileImageUrl())
                    .build();
        }

        // 2. 사용자가 마니띠인지 확인 (manittee_id로 매칭된 레코드가 있는지 확인)
        List<Manitto> isManitteeList = manittoRepository.findByManitteeIdAndGroupIdAndWeek(userId, groupId, currentWeek);

        if (!isManitteeList.isEmpty()) {
            // 사용자가 마니띠인 경우 - 담당 마니또의 익명 이름 반환
            Manitto manitto = isManitteeList.get(0);
            Long manittoId = manitto.getManitto().getId();

            // 마니또의 익명 이름 조회
            String manittoAnonymousName = anonymousNameRepository
                    .findAnonymousNameByUserIdAndGroupIdAndWeek(manittoId, groupId, currentWeek)
                    .orElse("익명의 마니또"); // 기본값

            return ManittoInfoResponseDto.builder()
                    .role("manittee")
                    .remainingTime(remainingTime)
                    .manittoAnonymousName(manittoAnonymousName)
                    .build();
        }

        // 3. 매칭이 아예 없는 경우
        return ManittoInfoResponseDto.builder()
                .role("none")
                .remainingTime(remainingTime)
                .build();
    }

    /**
     * MVP 호환성을 위한 오버로드 메서드 (기본 그룹 ID 1 사용)
     * @deprecated -> MVP 이후는 getCurrentManittoDetail 메소드 사용
     */
    @Deprecated
    @Transactional(readOnly = true)
    public ManittoInfoResponseDto getCurrentManittoInfo(Long userId) {
        return getCurrentManittoInfo(userId, 1L);
    }

    /**
     * 마니또 미션 상태 조회 (그룹 ID 파라미터 추가)
     */
    @Transactional
    public MissionStatusResponseDto getMissionStatus(Long userId, Long groupId) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 사용자가 해당 그룹에 속해있는지 확인
        if (!userGroupRepository.existsByUserIdAndGroupId(userId, groupId)) {
            throw new CustomException(ErrorCode.GROUP_NOT_FOUND, "해당 그룹에 속하지 않은 사용자입니다.");
        }

        // 현재 주차 계산
        int currentWeek = WeekCalculator.getCurrentWeek();
        LocalDate today = LocalDate.now();

        // 하루가 지난 미션을 미완료 상태로 처리 (현재 주차만)
        handleExpiredMissions(userId, groupId, today, currentWeek);

        // 현재 주차에 해당하는 사용자 미션 조회
        List<UserMission> userMissions = userMissionRepository.findByUserIdAndGroupIdAndWeek(userId, groupId, currentWeek);

        // 진행 중인 미션, 완료된 미션, 미완료 미션 구분
        List<UserMission> completedMissions = new ArrayList<>();
        List<UserMission> inProgressMissions = new ArrayList<>();
        List<UserMission> incompleteMissions = new ArrayList<>();

        for (UserMission userMission : userMissions) {
            // 미션 상태를 통해 판단
            if ("completed".equals(userMission.getStatus())) {
                completedMissions.add(userMission);
            } else if ("incomplete".equals(userMission.getStatus())) {
                incompleteMissions.add(userMission);
            } else {
                // 오늘 할당된 미션만 inProgress에 유지
                if (userMission.getAssignedDate().equals(today)) {
                    // 게시글 작성 여부로 상태 추가 판단
                    int postCount = postRepository.countByUserIdAndMissionIdAndWeekAndGroupId(
                            userId, userMission.getMission().getId(), currentWeek, groupId);

                    log.info("미션 완료 확인: userId={}, missionId={}, week={}, groupId={}, postCount={}",
                            userId, userMission.getMission().getId(), currentWeek, groupId, postCount);

                    if (postCount > 0) {
                        // 게시글이 있으면 미션을 완료 상태로 업데이트하고 완료 목록에 추가
                        userMission.complete();
                        userMissionRepository.save(userMission);
                        completedMissions.add(userMission);
                    } else {
                        inProgressMissions.add(userMission);
                    }
                } else {
                    // 오늘이 아닌 날짜에 할당된 미션은 미완료로 상태 변경
                    userMission.markAsIncomplete();
                    userMissionRepository.save(userMission);
                    incompleteMissions.add(userMission);
                }
            }
        }

        // 진행 중인 미션이 없고 전체 미션이 5개 미만이면 새 미션 할당이 필요하다는 로그만 남김
        if (inProgressMissions.isEmpty() && userMissions.size() < 5) {
            log.info("새로운 미션 할당이 필요합니다. 사용자 ID: {}, 그룹 ID: {}", userId, groupId);
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
        int incomplete = incompleteMissions.size();

        return MissionStatusResponseDto.builder()
                .progress(MissionStatusResponseDto.ProgressDto.builder()
                        .completed(completed)
                        .incomplete(incomplete)
                        .total(total)
                        .build())
                .missions(MissionStatusResponseDto.MissionsDto.builder()
                        .inProgress(inProgressMissionDtos)
                        .completed(completedMissionDtos)
                        .incomplete(incompleteMissionDtos)
                        .build())
                .build();
    }

    /**
     * MVP 호환성을 위한 오버로드 메서드 (기본 그룹 ID 1 사용)
     */
    @Transactional
    public MissionStatusResponseDto getMissionStatus(Long userId) {
        return getMissionStatus(userId, 1L);
    }

    /**
     * 새 미션 할당 (그룹 ID 파라미터 추가)
     */
    @Transactional
    public UserMission assignNewMission(Long userId, Long groupId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 사용자가 해당 그룹에 속해있는지 확인
        if (!userGroupRepository.existsByUserIdAndGroupId(userId, groupId)) {
            throw new CustomException(ErrorCode.GROUP_NOT_FOUND, "해당 그룹에 속하지 않은 사용자입니다.");
        }

        int currentWeek = WeekCalculator.getCurrentWeek();
        LocalDate today = LocalDate.now();

        // 현재 주차의 이전 미션들을 미완료 상태로 변경
        handleExpiredMissions(userId, groupId, today, currentWeek);

        // 오늘 이미 할당된 미션이 있는지 확인 (상태와 관계없이)
        List<UserMission> todaysMissions = userMissionRepository.findAllMissionsAssignedOnDate(userId, groupId, today, currentWeek);
        if (!todaysMissions.isEmpty()) {
            throw new CustomException(ErrorCode.DAILY_MISSION_LIMIT_EXCEEDED, "하루에 한 개의 미션만 수행할 수 있습니다.");
        }

        // 현재 주차에 해당하는 마니또 매칭 정보 조회
        List<Manitto> manittoList = manittoRepository.findByManittoIdAndGroupIdAndWeek(userId, groupId, currentWeek);

        // 마니또 매칭 정보가 없는 경우 예외 발생
        if (manittoList.isEmpty()) {
            throw new CustomException(ErrorCode.MANITTO_NOT_FOUND, "마니또 매칭 정보가 없어 미션을 할당할 수 없습니다.");
        }

        // 현재 주차의 모든 미션 조회 (중복 방지용)
        List<UserMission> userMissions = userMissionRepository.findByUserIdAndGroupIdAndWeek(userId, groupId, currentWeek);

        // 할당 가능한 미션 찾기
        Mission mission = getAvailableMission(userMissions);
        if (mission == null) {
            throw new CustomException(ErrorCode.MISSION_NOT_FOUND, "할당 가능한 미션이 없습니다.");
        }

        // 미션 생성 및 저장
        UserMission userMission = UserMission.builder()
                .user(user)
                .groupId(groupId) // 파라미터로 받은 그룹 ID 사용
                .mission(mission)
                .week(currentWeek)
                .assignedDate(today) // 오늘 날짜로 할당
                .build();

        return userMissionRepository.save(userMission);
    }

    /**
     * MVP 호환성을 위한 오버로드 메서드 (기본 그룹 ID 1 사용)
     */
    @Transactional
    public UserMission assignNewMission(Long userId) {
        return assignNewMission(userId, 1L);
    }

    /**
     * 이전 날짜의 완료되지 않은 미션을 처리하는 메서드 (그룹 ID 파라미터 추가)
     */
    private void handleExpiredMissions(Long userId, Long groupId, LocalDate today, Integer currentWeek) {
        // 현재 주차의 이전 날짜에 할당된 미션 중 진행 중인 상태인 미션 조회
        List<UserMission> expiredMissions = userMissionRepository.findIncompleteMissionsBeforeDate(
                userId, groupId, today, currentWeek);

        if (!expiredMissions.isEmpty()) {
            for (UserMission mission : expiredMissions) {
                // 미완료 상태로 변경
                mission.markAsIncomplete();
                userMissionRepository.save(mission);
                log.info("미완료 미션 처리: userId={}, groupId={}, missionId={}, assignedDate={}",
                        userId, groupId, mission.getMission().getId(), mission.getAssignedDate());
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

        // 이전 주차의 모든 미션 데이터 삭제 (상태와 관계없이)
        List<UserMission> previousWeekMissions = userMissionRepository.findByWeek(previousWeek);
        if (!previousWeekMissions.isEmpty()) {
            userMissionRepository.deleteAll(previousWeekMissions);
            log.info("이전 주차 미션 초기화 완료: 삭제된 미션 개수={}", previousWeekMissions.size());
        }

        // 현재 주차에 이미 생성된 미션이 있으면 모두 삭제
        List<UserMission> currentWeekMissions = userMissionRepository.findByWeek(currentWeek);
        if (!currentWeekMissions.isEmpty()) {
            userMissionRepository.deleteAll(currentWeekMissions);
            log.info("현재 주차 미션 초기화 완료: 삭제된 미션 개수={}", currentWeekMissions.size());
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
     * 다음 마니또 매칭 공개(항상 월요일 오후 12시)까지 남은 시간 계산
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