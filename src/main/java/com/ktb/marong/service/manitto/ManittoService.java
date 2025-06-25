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
import com.ktb.marong.dto.response.mission.TodayMissionResponseDto;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final GroupRepository groupRepository;

    /**
     * 현재 사용자의 마니또-마니띠 상세 정보 조회 (그룹별, 시간대별)
     * 신규 사용자 처리 포함
     */
    @Transactional(readOnly = true)
    public ManittoDetailResponseDto getCurrentManittoDetail(Long userId, Long groupId) {
        log.info("마니또 상세 정보 조회 시작: userId={}, groupId={}", userId, groupId);

        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 그룹 존재 여부 확인
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        // 3. 사용자가 해당 그룹에 속해있는지 확인
        UserGroup userGroup = userGroupRepository.findByUserIdAndGroupId(userId, groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND, "해당 그룹에 속하지 않은 사용자입니다."));

        // 4. 현재 주차 및 시간 정보
        int currentWeek = WeekCalculator.getCurrentWeek();
        String remainingTime = calculateRemainingTimeUntilReveal();
        String currentPeriod = getCurrentPeriod();
        boolean isNewUser = isNewUser(userId, groupId, currentWeek);

        log.info("현재 시간 정보: currentWeek={}, period={}, remainingTime={}, isNewUser={}",
                currentWeek, currentPeriod, remainingTime, isNewUser);

        // 5. 시간대에 따른 분기 처리
        if ("MANITTO_REVEAL".equals(currentPeriod)) {
            // 마니또 공개 기간 (금요일 17시 ~ 월요일 00시)
            return buildRevealPeriodResponse(userId, groupId, group, currentWeek, remainingTime, isNewUser);
        } else if ("MATCHING_PREPARATION".equals(currentPeriod)) {
            // 매칭 준비 기간 (월요일 00시 ~ 월요일 12시)
            return buildMatchingPreparationResponse(userId, groupId, group, currentWeek, remainingTime, isNewUser);
        } else {
            // 일반 활동 기간 (월요일 12시 ~ 금요일 17시)
            return buildActivePeriodResponse(userId, groupId, group, currentWeek, remainingTime, isNewUser);
        }
    }

    /**
     * 신규 사용자 여부 판단 (마니또 매칭 정보가 있는지 확인)
     */
    private boolean isNewUser(Long userId, Long groupId, int currentWeek) {
        // 현재 주차에 마니또 또는 마니띠로 매칭된 정보가 있는지 확인
        List<Manitto> asManitto = manittoRepository.findByManittoIdAndGroupIdAndWeek(userId, groupId, currentWeek);
        List<Manitto> asManittee = manittoRepository.findByManitteeIdAndGroupIdAndWeek(userId, groupId, currentWeek);

        boolean hasMatching = !asManitto.isEmpty() || !asManittee.isEmpty();

        log.info("신규 사용자 판단: userId={}, groupId={}, week={}, hasMatching={}",
                userId, groupId, currentWeek, hasMatching);

        return !hasMatching; // 매칭 정보가 없으면 신규 사용자
    }

    /**
     * 마니또 공개 기간 응답 생성 (금요일 17시 ~ 월요일 00시) -> MANITTO_REVEAL
     * 신규 사용자 처리 포함
     */
    private ManittoDetailResponseDto buildRevealPeriodResponse(Long userId, Long groupId, Group group,
                                                               int currentWeek, String remainingTime, boolean isNewUser) {
        log.info("마니또 공개 기간 응답 생성: userId={}, groupId={}, week={}, isNewUser={}", userId, groupId, currentWeek, isNewUser);

        ManittoDetailResponseDto.RevealedManittoDto revealedManitto = null;

        if (!isNewUser) {
            // 기존 사용자인 경우만 마니또 정보 조회
            List<Manitto> manitteeList = manittoRepository.findByManitteeIdAndGroupIdAndWeek(userId, groupId, currentWeek);

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

                // 그룹 닉네임이 없으면 카톡 실명으로 대체
                String revealedManittoGroupNickname = (manittoUserGroup != null && manittoUserGroup.getGroupUserNickname() != null)
                        ? manittoUserGroup.getGroupUserNickname()
                        : manittoUser.getNickname();

                revealedManitto = ManittoDetailResponseDto.RevealedManittoDto.builder()
                        .name(manittoUser.getNickname()) // 카카오 실명
                        .groupNickname(revealedManittoGroupNickname) // 그룹 닉네임 우선, 없으면 카톡 실명
                        .groupProfileImage(manittoUserGroup != null ? manittoUserGroup.getGroupUserProfileImageUrl() : null)
                        .anonymousName(manittoAnonymousName)
                        .build();

                log.info("공개된 마니또 정보: manittoUserId={}, name={}", manittoUser.getId(), manittoUser.getNickname());
            }
        } else {
            log.info("신규 사용자 - 공개할 마니또 정보 없음: userId={}, groupId={}, week={}", userId, groupId, currentWeek);
        }

        return ManittoDetailResponseDto.builder()
                .period("MANITTO_REVEAL")
                .remainingTime(remainingTime)
                .groupId(groupId)
                .groupName(group.getName())
                .isNewUser(isNewUser)
                .revealedManitto(revealedManitto)
                // 마니또 공개 기간에는 다른 필드들을 설정하지 않음 (null로 유지)
                .build();
    }

    /**
     * 매칭 준비 기간 응답 생성 (월요일 00시 ~ 월요일 12시) -> MATCHING_PREPARATION
     * 신규 사용자 처리 포함
     */
    private ManittoDetailResponseDto buildMatchingPreparationResponse(Long userId, Long groupId, Group group,
                                                                      int currentWeek, String remainingTime, boolean isNewUser) {
        log.info("매칭 준비 기간 응답 생성: userId={}, groupId={}, week={}, isNewUser={}", userId, groupId, currentWeek, isNewUser);

        ManittoDetailResponseDto.PreviousCycleManittoDto previousCycleManitto = null;

        if (!isNewUser) {
            // 기존 사용자인 경우만 지난주 마니또 정보 조회
            int previousWeek = currentWeek - 1;
            if (previousWeek > 0) {
                List<Manitto> previousManitteeList = manittoRepository.findByManitteeIdAndGroupIdAndWeek(userId, groupId, previousWeek);

                if (!previousManitteeList.isEmpty()) {
                    Manitto previousManittoRelation = previousManitteeList.get(0);
                    User previousManittoUser = previousManittoRelation.getManitto();

                    UserGroup previousManittoUserGroup = userGroupRepository.findByUserIdAndGroupId(previousManittoUser.getId(), groupId)
                            .orElse(null);

                    String previousManittoAnonymousName = anonymousNameRepository
                            .findAnonymousNameByUserIdAndGroupIdAndWeek(previousManittoUser.getId(), groupId, previousWeek)
                            .orElse("익명의 마니또");

                    // 그룹 닉네임이 없으면 카톡 실명으로 대체
                    String previousManittoGroupNickname = (previousManittoUserGroup != null && previousManittoUserGroup.getGroupUserNickname() != null)
                            ? previousManittoUserGroup.getGroupUserNickname()
                            : previousManittoUser.getNickname();

                    previousCycleManitto = ManittoDetailResponseDto.PreviousCycleManittoDto.builder()
                            .name(previousManittoUser.getNickname())
                            .groupNickname(previousManittoGroupNickname) // 그룹 닉네임 우선, 없으면 카톡 실명
                            .groupProfileImage(previousManittoUserGroup != null ? previousManittoUserGroup.getGroupUserProfileImageUrl() : null)
                            .anonymousName(previousManittoAnonymousName)
                            .build();

                    log.info("매칭 준비 기간 - 이전 주기 마니또 정보: userId={}, name={}, anonymousName={}",
                            previousManittoUser.getId(), previousManittoUser.getNickname(), previousManittoAnonymousName);
                }
            }
        } else {
            log.info("신규 사용자 - 매칭 준비 기간에 조회할 이전 마니또 정보 없음: userId={}, groupId={}", userId, groupId);
        }

        return ManittoDetailResponseDto.builder()
                .period("MATCHING_PREPARATION")
                .remainingTime(remainingTime)
                .groupId(groupId)
                .groupName(group.getName())
                .isNewUser(isNewUser)
                .previousCycleManitto(previousCycleManitto)
                // 매칭 준비 기간에는 나머지 필드들을 설정하지 않음 (null로 유지)
                .build();
    }

    /**
     * 일반 활동 기간 응답 생성 (월요일 12시 ~ 금요일 17시) -> MANITTO_ACTIVE
     * 신규 사용자 처리 포함
     */
    private ManittoDetailResponseDto buildActivePeriodResponse(Long userId, Long groupId, Group group,
                                                               int currentWeek, String remainingTime, boolean isNewUser) {
        log.info("일반 활동 기간 응답 생성: userId={}, groupId={}, week={}, isNewUser={}", userId, groupId, currentWeek, isNewUser);

        ManittoDetailResponseDto.PreviousCycleManittoDto previousCycleManitto = null;
        ManittoDetailResponseDto.CurrentManittoDto currentManitto = null;
        ManittoDetailResponseDto.CurrentManitteeDto currentManittee = null;

        if (!isNewUser) {
            // 기존 사용자인 경우만 마니또 정보들 조회

            // 이전 주기 마니또 정보
            int previousWeek = currentWeek - 1;
            if (previousWeek > 0) {
                List<Manitto> previousManitteeList = manittoRepository.findByManitteeIdAndGroupIdAndWeek(userId, groupId, previousWeek);

                if (!previousManitteeList.isEmpty()) {
                    Manitto previousManittoRelation = previousManitteeList.get(0);
                    User previousManittoUser = previousManittoRelation.getManitto();

                    UserGroup previousManittoUserGroup = userGroupRepository.findByUserIdAndGroupId(previousManittoUser.getId(), groupId)
                            .orElse(null);

                    String previousManittoAnonymousName = anonymousNameRepository
                            .findAnonymousNameByUserIdAndGroupIdAndWeek(previousManittoUser.getId(), groupId, previousWeek)
                            .orElse("익명의 마니또");

                    // 그룹 닉네임이 없으면 카톡 실명으로 대체
                    String previousManittoGroupNickname = (previousManittoUserGroup != null && previousManittoUserGroup.getGroupUserNickname() != null)
                            ? previousManittoUserGroup.getGroupUserNickname()
                            : previousManittoUser.getNickname();

                    previousCycleManitto = ManittoDetailResponseDto.PreviousCycleManittoDto.builder()
                            .name(previousManittoUser.getNickname())
                            .groupNickname(previousManittoGroupNickname) // 그룹 닉네임 우선, 없으면 카톡 실명
                            .groupProfileImage(previousManittoUserGroup != null ? previousManittoUserGroup.getGroupUserProfileImageUrl() : null)
                            .anonymousName(previousManittoAnonymousName)
                            .build();

                    log.info("이전 주기 마니또 정보: userId={}, name={}, anonymousName={}",
                            previousManittoUser.getId(), previousManittoUser.getNickname(), previousManittoAnonymousName);
                }
            }

            // 현재 나를 담당하는 마니또 정보
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
            List<Manitto> currentManittoList = manittoRepository.findByManittoIdAndGroupIdAndWeek(userId, groupId, currentWeek);

            if (!currentManittoList.isEmpty()) {
                Manitto currentManitteeRelation = currentManittoList.get(0);
                User currentManitteeUser = currentManitteeRelation.getManittee();

                UserGroup manitteeUserGroup = userGroupRepository.findByUserIdAndGroupId(currentManitteeUser.getId(), groupId)
                        .orElse(null);

                String displayName = (manitteeUserGroup != null && manitteeUserGroup.getGroupUserNickname() != null)
                        ? manitteeUserGroup.getGroupUserNickname()
                        : currentManitteeUser.getNickname();

                currentManittee = ManittoDetailResponseDto.CurrentManitteeDto.builder()
                        .name(currentManitteeUser.getNickname())
                        .groupNickname(displayName)
                        .groupProfileImage(manitteeUserGroup != null ? manitteeUserGroup.getGroupUserProfileImageUrl() : null)
                        .build();

                log.info("현재 마니띠 정보: manitteeUserId={}, name={}, groupNickname={}",
                        currentManitteeUser.getId(), currentManitteeUser.getNickname(), displayName);
            }
        } else {
            log.info("신규 사용자 - 모든 마니또 정보가 null로 설정됨: userId={}, groupId={}", userId, groupId);
        }

        return ManittoDetailResponseDto.builder()
                .period("MANITTO_ACTIVE")
                .remainingTime(remainingTime)
                .groupId(groupId)
                .groupName(group.getName())
                .isNewUser(isNewUser)
                .previousCycleManitto(previousCycleManitto)
                .currentManitto(currentManitto)
                .currentManittee(currentManittee)
                // 일반 활동 기간에는 revealedManitto를 설정하지 않음 (null로 유지)
                .build();
    }

    /**
     * 현재 시간이 어떤 기간에 속하는지 판단
     * @return "MANITTO_REVEAL", "MATCHING_PREPARATION", 또는 "MANITTO_ACTIVE"
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
        // 월요일 00시 ~ 12시 이전인 경우 (매칭 준비 기간)
        else if (dayOfWeek == DayOfWeek.MONDAY && hour < 12) {
            return "MATCHING_PREPARATION";
        }
        // 그 외의 경우 (월요일 12시 ~ 금요일 17시)
        else {
            return "MANITTO_ACTIVE";
        }
    }

    /**
     * MVP 호환용 - 현재 사용자의 마니또/마니띠 역할 및 정보 조회 (그룹 ID 파라미터 추가)
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
     * 마니또 미션 상태 조회 (그룹별 완전 분리)
     */
    @Transactional
    public MissionStatusResponseDto getMissionStatus(Long userId, Long groupId) {
        log.info("미션 상태 조회 시작: userId={}, groupId={}", userId, groupId);

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

        // 4. 현재 주차 및 날짜 정보
        int currentWeek = WeekCalculator.getCurrentWeek();
        LocalDate today = LocalDate.now();

        log.info("미션 상태 조회 정보: userId={}, groupId={}, currentWeek={}, today={}",
                userId, groupId, currentWeek, today);

        // 5. 해당 그룹에서 마니또 매칭이 되어 있는지 확인
        List<Manitto> manittoMatchings = manittoRepository.findByManittoIdAndGroupIdAndWeek(
                userId, groupId, currentWeek);

        if (manittoMatchings.isEmpty()) {
            log.warn("마니또 매칭 없음: userId={}, groupId={}, week={}", userId, groupId, currentWeek);
            throw new CustomException(ErrorCode.MANITTO_NOT_FOUND,
                    String.format("해당 그룹(ID: %d)에서 마니또 매칭 정보가 없습니다.", groupId));
        }

        // 6. 만료된 미션들을 미완료 상태로 처리 (해당 그룹만)
        handleExpiredMissionsForGroup(userId, groupId, today, currentWeek);

        // 7. 해당 그룹의 현재 주차 사용자 미션들만 조회
        List<UserMission> userMissions = userMissionRepository.findByUserIdAndGroupIdAndWeek(
                userId, groupId, currentWeek);

        log.info("조회된 그룹별 사용자 미션 개수: userId={}, groupId={}, week={}, count={}",
                userId, groupId, currentWeek, userMissions.size());

        // 8. 미션 상태별 분류 및 처리
        MissionClassificationResult classificationResult = classifyAndUpdateMissions(
                userId, groupId, currentWeek, today, userMissions);

        // 9. 새 미션 할당 필요성 체크 (그룹별)
        checkNewMissionAssignmentNeed(userId, groupId, currentWeek,
                classificationResult.getInProgressMissions(), userMissions.size());

        // 10. DTO 변환
        List<MissionStatusResponseDto.MissionDto> inProgressMissionDtos =
                classificationResult.getInProgressMissions().stream()
                        .map(this::convertToMissionDto)
                        .collect(Collectors.toList());

        List<MissionStatusResponseDto.MissionDto> completedMissionDtos =
                classificationResult.getCompletedMissions().stream()
                        .map(this::convertToMissionDto)
                        .collect(Collectors.toList());

        List<MissionStatusResponseDto.MissionDto> incompleteMissionDtos =
                classificationResult.getIncompleteMissions().stream()
                        .map(this::convertToMissionDto)
                        .collect(Collectors.toList());

        // 11. 진행률 계산
        int total = userMissions.size();
        int completed = classificationResult.getCompletedMissions().size();
        int incomplete = classificationResult.getIncompleteMissions().size();
        int inProgress = classificationResult.getInProgressMissions().size();

        log.info("그룹별 미션 상태 조회 완료: userId={}, groupId={}, total={}, completed={}, incomplete={}, inProgress={}",
                userId, groupId, total, completed, incomplete, inProgress);

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
     * 그룹별 만료된 미션 처리 (기존 handleExpiredMissions의 그룹별 버전)
     */
    private void handleExpiredMissionsForGroup(Long userId, Long groupId, LocalDate today, Integer currentWeek) {
        log.info("그룹별 만료된 미션 처리 시작: userId={}, groupId={}, today={}, week={}",
                userId, groupId, today, currentWeek);

        // 해당 그룹의 현재 주차에서 이전 날짜에 할당된 진행 중인 미션들 조회
        List<UserMission> expiredMissions = userMissionRepository.findIncompleteMissionsBeforeDate(
                userId, groupId, today, currentWeek);

        if (!expiredMissions.isEmpty()) {
            log.info("처리할 만료된 미션 개수: userId={}, groupId={}, count={}",
                    userId, groupId, expiredMissions.size());

            for (UserMission mission : expiredMissions) {
                // 게시글 작성 여부 최종 확인
                int postCount = postRepository.countByUserIdAndMissionIdAndWeekAndGroupId(
                        userId, mission.getMission().getId(), currentWeek, groupId);

                if (postCount > 0) {
                    // 게시글이 있으면 완료 처리
                    mission.complete();
                    log.info("만료 예정이었지만 게시글 발견으로 완료 처리: userId={}, groupId={}, missionId={}",
                            userId, groupId, mission.getMission().getId());
                } else {
                    // 게시글이 없으면 미완료 처리
                    mission.markAsIncomplete();
                    log.info("만료된 미션 미완료 처리: userId={}, groupId={}, missionId={}, assignedDate={}",
                            userId, groupId, mission.getMission().getId(), mission.getAssignedDate());
                }
                userMissionRepository.save(mission);
            }
        } else {
            log.info("처리할 만료된 미션 없음: userId={}, groupId={}", userId, groupId);
        }
    }

    /**
     * 미션 분류 및 상태 업데이트 처리
     */
    private MissionClassificationResult classifyAndUpdateMissions(Long userId, Long groupId,
                                                                  Integer currentWeek, LocalDate today, List<UserMission> userMissions) {

        log.info("미션 분류 및 상태 업데이트 시작: userId={}, groupId={}, missionsCount={}",
                userId, groupId, userMissions.size());

        List<UserMission> completedMissions = new ArrayList<>();
        List<UserMission> inProgressMissions = new ArrayList<>();
        List<UserMission> incompleteMissions = new ArrayList<>();

        for (UserMission userMission : userMissions) {
            log.info("미션 처리 중: userId={}, groupId={}, missionId={}, status={}, assignedDate={}",
                    userId, groupId, userMission.getMission().getId(),
                    userMission.getStatus(), userMission.getAssignedDate());

            String currentStatus = userMission.getStatus();

            if ("completed".equals(currentStatus)) {
                // 이미 완료된 미션
                completedMissions.add(userMission);
                log.info("이미 완료된 미션: userId={}, groupId={}, missionId={}",
                        userId, groupId, userMission.getMission().getId());

            } else if ("incomplete".equals(currentStatus)) {
                // 이미 미완료 처리된 미션
                incompleteMissions.add(userMission);
                log.info("이미 미완료된 미션: userId={}, groupId={}, missionId={}",
                        userId, groupId, userMission.getMission().getId());

            } else if ("ing".equals(currentStatus)) {
                // 진행 중인 미션 - 상태 확인 및 업데이트 필요

                if (userMission.getAssignedDate().equals(today)) {
                    // 오늘 할당된 미션 - 게시글 작성 여부 확인
                    int postCount = postRepository.countByUserIdAndMissionIdAndWeekAndGroupId(
                            userId, userMission.getMission().getId(), currentWeek, groupId);

                    log.info("오늘 미션 게시글 확인: userId={}, groupId={}, missionId={}, postCount={}",
                            userId, groupId, userMission.getMission().getId(), postCount);

                    if (postCount > 0) {
                        // 게시글이 있으면 완료 처리
                        userMission.complete();
                        userMissionRepository.save(userMission);
                        completedMissions.add(userMission);
                        log.info("오늘 미션 완료 처리: userId={}, groupId={}, missionId={}",
                                userId, groupId, userMission.getMission().getId());
                    } else {
                        // 게시글이 없으면 여전히 진행 중
                        inProgressMissions.add(userMission);
                        log.info("오늘 미션 진행 중 유지: userId={}, groupId={}, missionId={}",
                                userId, groupId, userMission.getMission().getId());
                    }

                } else if (userMission.getAssignedDate().isBefore(today)) {
                    // 과거에 할당된 미션 - 미완료 처리 (이미 handleExpiredMissionsForGroup에서 처리되었지만 확인)
                    userMission.markAsIncomplete();
                    userMissionRepository.save(userMission);
                    incompleteMissions.add(userMission);
                    log.info("과거 미션 미완료 처리: userId={}, groupId={}, missionId={}, assignedDate={}",
                            userId, groupId, userMission.getMission().getId(), userMission.getAssignedDate());

                } else {
                    // 미래 날짜 미션 (일반적으로 발생하지 않아야 함)
                    log.warn("미래 날짜 미션 발견: userId={}, groupId={}, missionId={}, assignedDate={}",
                            userId, groupId, userMission.getMission().getId(), userMission.getAssignedDate());
                    inProgressMissions.add(userMission);
                }
            } else {
                // 예상치 못한 상태
                log.warn("예상치 못한 미션 상태: userId={}, groupId={}, missionId={}, status={}",
                        userId, groupId, userMission.getMission().getId(), currentStatus);
                inProgressMissions.add(userMission); // 기본적으로 진행 중으로 처리
            }
        }

        log.info("미션 분류 완료: userId={}, groupId={}, completed={}, inProgress={}, incomplete={}",
                userId, groupId, completedMissions.size(), inProgressMissions.size(), incompleteMissions.size());

        return new MissionClassificationResult(completedMissions, inProgressMissions, incompleteMissions);
    }

    /**
     * 새 미션 할당 필요성 체크
     */
    private void checkNewMissionAssignmentNeed(Long userId, Long groupId, Integer currentWeek,
                                               List<UserMission> inProgressMissions, int totalMissions) {

        // 진행 중인 미션이 없고 전체 미션이 최대 주간 미션 수(5개) 미만이면 새 미션 할당 권장
        final int MAX_WEEKLY_MISSIONS = 5; // 하나의 매칭 주기 동안 최대 수행 가능한 미션 수

        if (inProgressMissions.isEmpty() && totalMissions < MAX_WEEKLY_MISSIONS) {
            log.info("새로운 미션 할당 권장: userId={}, groupId={}, week={}, currentMissionCount={}, maxWeeklyMissions={}",
                    userId, groupId, currentWeek, totalMissions, MAX_WEEKLY_MISSIONS);
        } else if (inProgressMissions.isEmpty() && totalMissions >= MAX_WEEKLY_MISSIONS) {
            log.info("주간 미션 한도 달성: userId={}, groupId={}, week={}, totalMissions={}",
                    userId, groupId, currentWeek, totalMissions);
        } else {
            log.info("진행 중인 미션 존재: userId={}, groupId={}, inProgressCount={}",
                    userId, groupId, inProgressMissions.size());
        }
    }

    /**
     * 미션 분류 결과를 담는 내부 클래스
     */
    private static class MissionClassificationResult {
        private final List<UserMission> completedMissions;
        private final List<UserMission> inProgressMissions;
        private final List<UserMission> incompleteMissions;

        public MissionClassificationResult(List<UserMission> completedMissions,
                                           List<UserMission> inProgressMissions,
                                           List<UserMission> incompleteMissions) {
            this.completedMissions = completedMissions;
            this.inProgressMissions = inProgressMissions;
            this.incompleteMissions = incompleteMissions;
        }

        public List<UserMission> getCompletedMissions() { return completedMissions; }
        public List<UserMission> getInProgressMissions() { return inProgressMissions; }
        public List<UserMission> getIncompleteMissions() { return incompleteMissions; }
    }

    /**
     * MVP 호환성을 위한 오버로드 메서드 (기본 그룹 ID 1 사용)
     * @deprecated -> MVP 이후는 getMissionStatus 사용
     */
    @Deprecated
    @Transactional
    public MissionStatusResponseDto getMissionStatus(Long userId) {
        log.warn("레거시 미션 상태 조회 메소드 사용: userId={} - 기본 그룹(ID: 1) 사용", userId);
        return getMissionStatus(userId, 1L);
    }

    // 추가 API
    /**
     * 그룹별 미션 통계 조회
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getGroupMissionStatistics(Long userId, Long groupId) {
        log.info("그룹별 미션 통계 조회: userId={}, groupId={}", userId, groupId);

        // 그룹 존재 여부 확인
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        // 사용자가 해당 그룹에 속해있는지 확인
        if (!userGroupRepository.existsByUserIdAndGroupId(userId, groupId)) {
            throw new CustomException(ErrorCode.GROUP_NOT_FOUND, "해당 그룹에 속하지 않은 사용자입니다.");
        }

        int currentWeek = WeekCalculator.getCurrentWeek();

        // 그룹별 현재 주차 미션 통계
        long completedCount = userMissionRepository.countMissionsByUserGroupWeekAndStatus(
                userId, groupId, currentWeek, "completed");
        long inProgressCount = userMissionRepository.countMissionsByUserGroupWeekAndStatus(
                userId, groupId, currentWeek, "ing");
        long incompleteCount = userMissionRepository.countMissionsByUserGroupWeekAndStatus(
                userId, groupId, currentWeek, "incomplete");

        long totalCount = completedCount + inProgressCount + incompleteCount;

        // 완료율 계산
        double completionRate = totalCount > 0 ? (double) completedCount / totalCount * 100 : 0.0;

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("groupId", groupId);
        statistics.put("week", currentWeek);
        statistics.put("totalMissions", totalCount);
        statistics.put("completedMissions", completedCount);
        statistics.put("inProgressMissions", inProgressCount);
        statistics.put("incompleteMissions", incompleteCount);
        statistics.put("completionRate", Math.round(completionRate * 100.0) / 100.0); // 소수점 2자리

        log.info("그룹별 미션 통계: userId={}, groupId={}, stats={}", userId, groupId, statistics);

        return statistics;
    }

    /**
     * 여러 그룹의 미션 통계 비교 조회
     */
    @Transactional(readOnly = true)
    public Map<String, Object> compareGroupMissionStatistics(Long userId, List<Long> groupIds) {
        log.info("여러 그룹 미션 통계 비교: userId={}, groupIds={}", userId, groupIds);

        Map<String, Object> comparison = new HashMap<>();
        List<Map<String, Object>> groupStatistics = new ArrayList<>();

        for (Long groupId : groupIds) {
            try {
                Map<String, Object> groupStats = getGroupMissionStatistics(userId, groupId);
                groupStatistics.add(groupStats);
            } catch (CustomException e) {
                log.warn("그룹 통계 조회 실패 건너뛰기: userId={}, groupId={}, error={}",
                        userId, groupId, e.getMessage());
            }
        }

        comparison.put("userId", userId);
        comparison.put("comparisonDate", LocalDate.now());
        comparison.put("groupStatistics", groupStatistics);
        comparison.put("totalGroupsCompared", groupStatistics.size());

        return comparison;
    }

    /**
     * 새 미션 할당 (그룹 ID 파라미터 추가) - 랜덤 미션 할당
     * 시스템이 자동으로 사용자에게 적합한 미션을 랜덤으로 할당
     */
    @Transactional
    public UserMission assignNewMission(Long userId, Long groupId) {
        log.info("새 미션 할당 요청: userId={}, groupId={}", userId, groupId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 1. 그룹 존재 여부 확인
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        // 2. 사용자가 해당 그룹에 속해있는지 확인
        if (!userGroupRepository.existsByUserIdAndGroupId(userId, groupId)) {
            throw new CustomException(ErrorCode.GROUP_NOT_FOUND, "해당 그룹에 속하지 않은 사용자입니다.");
        }

        int currentWeek = WeekCalculator.getCurrentWeek();
        LocalDate today = LocalDate.now();

        // 3. 현재 주차의 이전 미션들을 미완료 상태로 변경
        handleExpiredMissionsForGroup(userId, groupId, today, currentWeek);

        // 4. 오늘 이미 할당된 미션이 있는지 확인 (상태와 관계없이)
        List<UserMission> todaysMissions = userMissionRepository.findAllMissionsAssignedOnDate(userId, groupId, today, currentWeek);
        if (!todaysMissions.isEmpty()) {
            throw new CustomException(ErrorCode.DAILY_MISSION_LIMIT_EXCEEDED, "하루에 한 개의 미션만 수행할 수 있습니다.");
        }

        // 5. 현재 주차에 해당하는 마니또 매칭 정보 조회
        List<Manitto> manittoList = manittoRepository.findByManittoIdAndGroupIdAndWeek(userId, groupId, currentWeek);

        // 6. 마니또 매칭 정보가 없는 경우 예외 발생
        if (manittoList.isEmpty()) {
            throw new CustomException(ErrorCode.MANITTO_NOT_FOUND, "마니또 매칭 정보가 없어 미션을 할당할 수 없습니다.");
        }

        // 7. 현재 주차의 모든 미션 조회 (중복 방지용)
        List<UserMission> userMissions = userMissionRepository.findByUserIdAndGroupIdAndWeek(userId, groupId, currentWeek);

        // 8. 할당 가능한 미션 찾기 (그룹별로 독립적)
        Mission mission = getAvailableMissionForGroup(userId, groupId, userMissions);
        if (mission == null) {
            throw new CustomException(ErrorCode.MISSION_NOT_FOUND, "할당 가능한 미션이 없습니다.");
        }

        // 9. 미션 생성 및 저장
        UserMission userMission = UserMission.builder()
                .user(user)
                .groupId(groupId)
                .mission(mission)
                .week(currentWeek)
                .assignedDate(today)
                .build();

        UserMission savedMission = userMissionRepository.save(userMission);

        log.info("새 미션 할당 완료: userId={}, groupId={}, missionId={}, userMissionId={}",
                userId, groupId, mission.getId(), savedMission.getId());

        return savedMission;
    }

    /**
     * MVP 호환성을 위한 오버로드 메서드 (기본 그룹 ID 1 사용)
     */
    @Deprecated
    @Transactional
    public UserMission assignNewMission(Long userId) {
        return assignNewMission(userId, 1L);
    }

    /**
     * 그룹별 오늘 할당된 미션 조회
     */
    @Transactional(readOnly = true)
    public TodayMissionResponseDto getTodayAssignedMission(Long userId, Long groupId) {
        log.info("오늘 할당된 미션 조회: userId={}, groupId={}", userId, groupId);

        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 그룹 존재 여부 확인
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        // 3. 사용자가 해당 그룹에 속해있는지 확인
        if (!userGroupRepository.existsByUserIdAndGroupId(userId, groupId)) {
            throw new CustomException(ErrorCode.GROUP_NOT_FOUND, "해당 그룹에 속하지 않은 사용자입니다.");
        }

        int currentWeek = WeekCalculator.getCurrentWeek();
        LocalDate today = LocalDate.now();

        // 4. 오늘 할당된 미션 조회 (상태와 관계없이)
        List<UserMission> todaysMissions = userMissionRepository.findAllMissionsAssignedOnDate(
                userId, groupId, today, currentWeek);

        if (todaysMissions.isEmpty()) {
            log.info("오늘 할당된 미션 없음: userId={}, groupId={}, date={}", userId, groupId, today);
            return null; // 204 No Content 응답을 위해 null 반환
        }

        // 5. 첫 번째 미션 반환 (하루에 1개만 할당되므로)
        UserMission todaysMission = todaysMissions.get(0);

        // 6. 게시글 작성 여부 확인하여 상태 업데이트
        int postCount = postRepository.countByUserIdAndMissionIdAndWeekAndGroupId(
                userId, todaysMission.getMission().getId(), currentWeek, groupId);

        if (postCount > 0 && !"completed".equals(todaysMission.getStatus())) {
            // 게시글이 있는데 아직 완료 상태가 아니면 완료로 업데이트
            todaysMission.complete();
            userMissionRepository.save(todaysMission);
        }

        log.info("오늘 할당된 미션 조회 완료: userId={}, groupId={}, missionId={}, status={}",
                userId, groupId, todaysMission.getMission().getId(), todaysMission.getStatus());

        return TodayMissionResponseDto.builder()
                .missionId(todaysMission.getMission().getId())
                .title(todaysMission.getMission().getTitle())
                .description(todaysMission.getMission().getDescription())
                .difficulty(todaysMission.getMission().getDifficulty())
                .selectedAt(todaysMission.getAssignedDate())
                .status(todaysMission.getStatus())
                .build();
    }

    /**
     * 그룹별로 할당 가능한 미션 찾기 (중복 방지)
     */
    private Mission getAvailableMissionForGroup(Long userId, Long groupId, List<UserMission> existingMissions) {
        log.info("그룹별 할당 가능한 미션 찾기: userId={}, groupId={}, existingMissionsCount={}",
                userId, groupId, existingMissions.size());

        // 이미 할당된 미션의 ID 목록 (해당 그룹에서)
        List<Long> assignedMissionIds = existingMissions.stream()
                .map(um -> um.getMission().getId())
                .collect(Collectors.toList());

        log.info("이미 할당된 미션 IDs: {}", assignedMissionIds);

        // 전체 미션 중 아직 할당되지 않은 미션 찾기
        List<Mission> allMissions = missionRepository.findAll();
        List<Mission> availableMissions = allMissions.stream()
                .filter(mission -> !assignedMissionIds.contains(mission.getId()))
                .collect(Collectors.toList());

        log.info("할당 가능한 미션 개수: {}", availableMissions.size());

        if (availableMissions.isEmpty()) {
            log.warn("할당 가능한 미션이 없음: userId={}, groupId={}", userId, groupId);
            return null;
        }

        // 랜덤으로 하나 선택
        int randomIndex = (int) (Math.random() * availableMissions.size());
        Mission selectedMission = availableMissions.get(randomIndex);

        log.info("랜덤 미션 선택: userId={}, groupId={}, selectedMissionId={}, title={}",
                userId, groupId, selectedMission.getId(), selectedMission.getTitle());

        return selectedMission;
    }

    /**
     * MVP 호환용
     * @deprecated -> MVP 이후는 handleExpiredMissionsForGroup 사용
     */
    @Deprecated
    private void handleExpiredMissions(Long userId, Long groupId, LocalDate today, Integer currentWeek) {
        log.warn("레거시 handleExpiredMissions 메소드 사용: userId={}, groupId={}", userId, groupId);
        handleExpiredMissionsForGroup(userId, groupId, today, currentWeek);
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
     * MVP 호환용 - 사용자에게 아직 할당되지 않은 미션 찾기
     * @deprecated -> MVP 이후는 getAvailableMissionForGroup 사용
     */
    @Deprecated
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