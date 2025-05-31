package com.ktb.marong.controller;

import com.ktb.marong.domain.mission.UserMission;
import com.ktb.marong.dto.response.common.ApiResponse;
import com.ktb.marong.dto.response.manitto.ManittoDetailResponseDto;
import com.ktb.marong.dto.response.manitto.ManittoInfoResponseDto;
import com.ktb.marong.dto.response.mission.TodayMissionResponseDto;
import com.ktb.marong.exception.CustomException;
import com.ktb.marong.security.CurrentUser;
import com.ktb.marong.service.manitto.ManittoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/manitto")
public class ManittoController {

    private final ManittoService manittoService;

    /**
     * 마니또 상세 정보 조회 (그룹별, 시간대별)
     */
    @GetMapping("/detail")
    public ResponseEntity<?> getManittoDetail(
            @CurrentUser Long userId,
            @RequestParam Long groupId) {

        log.info("마니또 상세 정보 요청: userId={}, groupId={}", userId, groupId);

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
     * 마니또 미션 상태 조회 (그룹 ID 파라미터 추가)
     */
    @GetMapping("/missions")
    public ResponseEntity<?> getMissionStatus(
            @CurrentUser Long userId,
            @RequestParam(required = false) Long groupId) {

        // groupId가 없으면 기본 그룹(1) 사용 (MVP 호환성)
        Long targetGroupId = (groupId != null) ? groupId : 1L;

        log.info("미션 상태 요청: userId={}, groupId={}", userId, targetGroupId);

        try {
            var response = manittoService.getMissionStatus(userId, targetGroupId);

            return ResponseEntity.ok(ApiResponse.success(
                    response,
                    "mission_status_retrieved",
                    null
            ));
        } catch (CustomException e) {
            log.error("미션 상태 조회 실패: userId={}, groupId={}, error={}",
                    userId, targetGroupId, e.getMessage());
            return ResponseEntity.status(e.getErrorCode().getStatus())
                    .body(ApiResponse.error(e.getErrorCode().name(), e.getMessage()));
        } catch (Exception e) {
            log.error("미션 상태 조회 중 예상치 못한 오류: userId={}, groupId={}",
                    userId, targetGroupId, e);
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
}