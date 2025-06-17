package com.ktb.marong.controller;

import com.ktb.marong.domain.mission.UserMission;
import com.ktb.marong.dto.response.common.ApiResponse;
import com.ktb.marong.dto.response.manitto.ManittoDetailResponseDto;
import com.ktb.marong.dto.response.manitto.ManittoInfoResponseDto;
import com.ktb.marong.dto.response.manitto.MissionStatusResponseDto;
import com.ktb.marong.dto.response.mission.TodayMissionResponseDto;
import com.ktb.marong.exception.CustomException;
import com.ktb.marong.repository.GroupRepository;
import com.ktb.marong.security.CurrentUser;
import com.ktb.marong.service.manitto.ManittoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/manitto")
public class ManittoController {

    private final ManittoService manittoService;
    private final GroupRepository groupRepository;

    /**
     * 마니또 상세 정보 조회 (그룹별, 시간대별)
     */
    @GetMapping("/detail")
    public ResponseEntity<?> getManittoDetail(
            @CurrentUser Long userId,
            @RequestParam Long groupId) {

        log.info("마니또 상세 정보 요청: userId={}, groupId={}", userId, groupId);

        // 그룹 존재 여부 확인
        if (!groupRepository.existsById(groupId)) {
            log.warn("존재하지 않는 그룹: groupId={}", groupId);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("GROUP_NOT_FOUND", "존재하지 않는 그룹입니다."));
        }

        try {
            ManittoDetailResponseDto response = manittoService.getCurrentManittoDetail(userId, groupId);

            return ResponseEntity.ok(ApiResponse.success(
                    response,
                    "manitto_detail_retrieved",
                    null
            ));
        } catch (CustomException e) {
            log.error("마니또 상세 정보 조회 실패: userId={}, groupId={}, error={}",
                    userId, groupId, e.getMessage());
            return ResponseEntity.status(e.getErrorCode().getStatus())
                    .body(ApiResponse.error(e.getErrorCode().name(), e.getMessage()));
        } catch (Exception e) {
            log.error("마니또 상세 정보 조회 중 예상치 못한 오류: userId={}, groupId={}",
                    userId, groupId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "서버 오류입니다."));
        }
    }

    /**
     * 현재 마니또가 담당하는 마니띠 정보 조회 (그룹 ID 파라미터 추가)
     * @deprecated -> MVP 이후는 getManittoDetail API 사용
     */
    @Deprecated
    @GetMapping
    public ResponseEntity<?> getCurrentManitto(
            @CurrentUser Long userId,
            @RequestParam(required = false) Long groupId) {

        // groupId가 없으면 기본 그룹(1) 사용 (MVP 호환성)
        Long targetGroupId = (groupId != null) ? groupId : 1L;

        log.info("마니또가 담당하는 마니띠 정보 요청: userId={}, groupId={}", userId, targetGroupId);

        try {
            ManittoInfoResponseDto response = manittoService.getCurrentManittoInfo(userId, targetGroupId);

            return ResponseEntity.ok(ApiResponse.success(
                    response,
                    "manitto_info_retrieved",
                    null
            ));
        } catch (CustomException e) {
            log.error("마니또 정보 조회 실패: userId={}, groupId={}, error={}",
                    userId, targetGroupId, e.getMessage());
            return ResponseEntity.status(e.getErrorCode().getStatus())
                    .body(ApiResponse.error(e.getErrorCode().name(), e.getMessage()));
        } catch (Exception e) {
            log.error("마니또 정보 조회 중 예상치 못한 오류: userId={}, groupId={}",
                    userId, targetGroupId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "서버 오류입니다."));
        }
    }

    /**
     * 마니또 미션 상태 조회
     */
    @GetMapping("/missions")
    public ResponseEntity<?> getMissionStatus(
            @CurrentUser Long userId,
            @RequestParam Long groupId) {

        log.info("미션 상태 요청: userId={}, groupId={}", userId, groupId);

        // groupId 유효성 검증
        if (groupId == null || groupId <= 0) {
            log.warn("잘못된 groupId: userId={}, groupId={}", userId, groupId);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_GROUP_ID", "유효하지 않은 그룹 ID입니다."));
        }

        // 그룹 존재 여부 확인
        if (!groupRepository.existsById(groupId)) {
            log.warn("존재하지 않는 그룹: groupId={}", groupId);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("GROUP_NOT_FOUND", "존재하지 않는 그룹입니다."));
        }

        try {
            MissionStatusResponseDto response = manittoService.getMissionStatus(userId, groupId);

            return ResponseEntity.ok(ApiResponse.success(
                    response,
                    "mission_status_retrieved",
                    null
            ));
        } catch (CustomException e) {
            log.error("미션 상태 조회 실패: userId={}, groupId={}, error={}",
                    userId, groupId, e.getMessage());
            return ResponseEntity.status(e.getErrorCode().getStatus())
                    .body(ApiResponse.error(e.getErrorCode().name(), e.getMessage()));
        } catch (Exception e) {
            log.error("미션 상태 조회 중 예상치 못한 오류: userId={}, groupId={}",
                    userId, groupId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "서버 오류입니다."));
        }
    }

    /**
     * MVP 호환성을 위한 기본 그룹 미션 상태 조회
     * @deprecated -> MVP 이후는 getMissionStatus 사용
     */
    @Deprecated
    @GetMapping("/missions/legacy")
    public ResponseEntity<?> getMissionStatusLegacy(@CurrentUser Long userId) {
        log.warn("레거시 미션 상태 조회 사용: userId={} - 기본 그룹(ID: 1) 사용", userId);

        try {
            // 기본 그룹 ID 1 사용 (MVP 호환성)
            MissionStatusResponseDto response = manittoService.getMissionStatus(userId, 1L);

            return ResponseEntity.ok(ApiResponse.success(
                    response,
                    "mission_status_retrieved",
                    null
            ));
        } catch (CustomException e) {
            log.error("레거시 미션 상태 조회 실패: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.status(e.getErrorCode().getStatus())
                    .body(ApiResponse.error(e.getErrorCode().name(), e.getMessage()));
        } catch (Exception e) {
            log.error("레거시 미션 상태 조회 중 예상치 못한 오류: userId={}", userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "서버 오류입니다."));
        }
    }

    /**
     * 새 미션 할당 (그룹 ID 파라미터 추가)
     */
    @PostMapping("/missions/assign")
    public ResponseEntity<?> assignNewMission(
            @CurrentUser Long userId,
            @RequestParam(required = false) Long groupId) {

        // groupId가 없으면 기본 그룹(1) 사용 (MVP 호환성)
        Long targetGroupId = (groupId != null) ? groupId : 1L;

        log.info("새 미션 할당 요청: userId={}, groupId={}", userId, targetGroupId);

        // 그룹 존재 여부 확인
        if (!groupRepository.existsById(targetGroupId)) {
            log.warn("존재하지 않는 그룹: groupId={}", targetGroupId);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("GROUP_NOT_FOUND", "존재하지 않는 그룹입니다."));
        }

        try {
            UserMission newMission = manittoService.assignNewMission(userId, targetGroupId);

            return ResponseEntity.ok(ApiResponse.success(
                    Map.of(
                            "missionId", newMission.getMission().getId(),
                            "title", newMission.getMission().getTitle(),
                            "description", newMission.getMission().getDescription(),
                            "difficulty", newMission.getMission().getDifficulty(),
                            "groupId", targetGroupId,
                            "assignedDate", newMission.getAssignedDate()
                    ),
                    "mission_assigned",
                    null
            ));
        } catch (CustomException e) {
            log.error("미션 할당 실패: userId={}, groupId={}, error={}",
                    userId, targetGroupId, e.getMessage());
            return ResponseEntity.status(e.getErrorCode().getStatus())
                    .body(ApiResponse.error(e.getErrorCode().name(), e.getMessage()));
        } catch (Exception e) {
            log.error("미션 할당 중 예상치 못한 오류: userId={}, groupId={}",
                    userId, targetGroupId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "서버 오류입니다."));
        }
    }

    /**
     * 그룹별 오늘 할당된 미션 조회
     */
    @GetMapping("/missions/today")
    public ResponseEntity<?> getTodayAssignedMission(
            @CurrentUser Long userId,
            @RequestParam Long groupId) {

        log.info("오늘 할당된 미션 조회: userId={}, groupId={}", userId, groupId);

        // 그룹 존재 여부 확인
        if (!groupRepository.existsById(groupId)) {
            log.warn("존재하지 않는 그룹: groupId={}", groupId);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("GROUP_NOT_FOUND", "존재하지 않는 그룹입니다."));
        }

        try {
            TodayMissionResponseDto response = manittoService.getTodayAssignedMission(userId, groupId);

            if (response == null) {
                log.info("오늘 할당된 미션 없음: userId={}, groupId={}", userId, groupId);
                return ResponseEntity.noContent().build(); // 204 No Content
            }

            return ResponseEntity.ok(ApiResponse.success(
                    response,
                    "today_mission_retrieved",
                    null
            ));
        } catch (CustomException e) {
            log.error("오늘 미션 조회 실패: userId={}, groupId={}, error={}",
                    userId, groupId, e.getMessage());
            return ResponseEntity.status(e.getErrorCode().getStatus())
                    .body(ApiResponse.error(e.getErrorCode().name(), e.getMessage()));
        } catch (Exception e) {
            log.error("오늘 미션 조회 중 예상치 못한 오류: userId={}, groupId={}",
                    userId, groupId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "서버 오류입니다."));
        }
    }

    // 추가 API
    /**
     * 그룹별 미션 통계 조회
     */
    @GetMapping("/missions/statistics")
    public ResponseEntity<?> getGroupMissionStatistics(
            @CurrentUser Long userId,
            @RequestParam Long groupId) {

        log.info("그룹별 미션 통계 요청: userId={}, groupId={}", userId, groupId);

        // groupId 유효성 검증
        if (groupId == null || groupId <= 0) {
            log.warn("잘못된 groupId: userId={}, groupId={}", userId, groupId);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_GROUP_ID", "유효하지 않은 그룹 ID입니다."));
        }

        // 그룹 존재 여부 확인
        if (!groupRepository.existsById(groupId)) {
            log.warn("존재하지 않는 그룹: groupId={}", groupId);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("GROUP_NOT_FOUND", "존재하지 않는 그룹입니다."));
        }

        try {
            Map<String, Object> statistics = manittoService.getGroupMissionStatistics(userId, groupId);

            return ResponseEntity.ok(ApiResponse.success(
                    statistics,
                    "mission_statistics_retrieved",
                    null
            ));
        } catch (CustomException e) {
            log.error("미션 통계 조회 실패: userId={}, groupId={}, error={}",
                    userId, groupId, e.getMessage());
            return ResponseEntity.status(e.getErrorCode().getStatus())
                    .body(ApiResponse.error(e.getErrorCode().name(), e.getMessage()));
        } catch (Exception e) {
            log.error("미션 통계 조회 중 예상치 못한 오류: userId={}, groupId={}",
                    userId, groupId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "서버 오류입니다."));
        }
    }

    /**
     * 여러 그룹 미션 통계 비교 조회
     */
    @GetMapping("/missions/statistics/compare")
    public ResponseEntity<?> compareGroupMissionStatistics(
            @CurrentUser Long userId,
            @RequestParam List<Long> groupIds) {

        log.info("여러 그룹 미션 통계 비교 요청: userId={}, groupIds={}", userId, groupIds);

        // groupIds 유효성 검증
        if (groupIds == null || groupIds.isEmpty()) {
            log.warn("빈 groupIds: userId={}", userId);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_GROUP_IDS", "그룹 ID 목록이 필요합니다."));
        }

        // 최대 5개 그룹까지만 비교 허용
        if (groupIds.size() > 5) {
            log.warn("너무 많은 그룹 비교 요청: userId={}, count={}", userId, groupIds.size());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TOO_MANY_GROUPS", "최대 5개 그룹까지만 비교할 수 있습니다."));
        }

        try {
            Map<String, Object> comparison = manittoService.compareGroupMissionStatistics(userId, groupIds);

            return ResponseEntity.ok(ApiResponse.success(
                    comparison,
                    "mission_statistics_compared",
                    null
            ));
        } catch (Exception e) {
            log.error("미션 통계 비교 중 예상치 못한 오류: userId={}, groupIds={}",
                    userId, groupIds, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "서버 오류입니다."));
        }
    }
}