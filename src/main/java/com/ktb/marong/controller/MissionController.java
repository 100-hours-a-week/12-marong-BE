package com.ktb.marong.controller;

import com.ktb.marong.dto.request.mission.SelectMissionRequestDto;
import com.ktb.marong.dto.response.common.ApiResponse;
import com.ktb.marong.dto.response.mission.AvailableMissionResponseDto;
import com.ktb.marong.dto.response.mission.SelectMissionResponseDto;
import com.ktb.marong.exception.CustomException;
import com.ktb.marong.repository.GroupRepository;
import com.ktb.marong.security.CurrentUser;
import com.ktb.marong.service.mission.MissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/missions")
public class MissionController {

    private final MissionService missionService;
    private final GroupRepository groupRepository;

    /**
     * 선택 가능한 미션 목록 조회
     */
    @GetMapping("/available")
    public ResponseEntity<?> getAvailableMissions(
            @CurrentUser Long userId,
            @RequestParam Long groupId) {

        log.info("선택 가능한 미션 목록 조회 요청: userId={}, groupId={}", userId, groupId);

        // 그룹 존재 여부 확인
        if (!groupRepository.existsById(groupId)) {
            log.warn("존재하지 않는 그룹: groupId={}", groupId);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("GROUP_NOT_FOUND", "존재하지 않는 그룹입니다."));
        }

        try {
            AvailableMissionResponseDto response = missionService.getAvailableMissions(userId, groupId);

            return ResponseEntity.ok(ApiResponse.success(
                    response,
                    "available_missions_retrieved",
                    null
            ));
        } catch (CustomException e) {
            log.error("선택 가능한 미션 목록 조회 실패: userId={}, groupId={}, error={}",
                    userId, groupId, e.getMessage());
            return ResponseEntity.status(e.getErrorCode().getStatus())
                    .body(ApiResponse.error(e.getErrorCode().name(), e.getMessage()));
        } catch (Exception e) {
            log.error("선택 가능한 미션 목록 조회 중 예상치 못한 오류: userId={}, groupId={}",
                    userId, groupId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "서버 오류입니다."));
        }
    }

    /**
     * 미션 선택
     */
    @PostMapping("/select")
    public ResponseEntity<?> selectMission(
            @CurrentUser Long userId,
            @Valid @RequestBody SelectMissionRequestDto requestDto) {

        log.info("미션 선택 요청: userId={}, missionId={}, groupId={}",
                userId, requestDto.getMissionId(), requestDto.getGroupId());

        // 그룹 존재 여부 확인
        if (!groupRepository.existsById(requestDto.getGroupId())) {
            log.warn("존재하지 않는 그룹: groupId={}", requestDto.getGroupId());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("GROUP_NOT_FOUND", "존재하지 않는 그룹입니다."));
        }

        try {
            SelectMissionResponseDto response = missionService.selectMission(userId, requestDto);

            return ResponseEntity.ok(ApiResponse.success(
                    response,
                    "mission_selected",
                    null
            ));
        } catch (CustomException e) {
            log.error("미션 선택 실패: userId={}, missionId={}, groupId={}, error={}",
                    userId, requestDto.getMissionId(), requestDto.getGroupId(), e.getMessage());
            return ResponseEntity.status(e.getErrorCode().getStatus())
                    .body(ApiResponse.error(e.getErrorCode().name(), e.getMessage()));
        } catch (Exception e) {
            log.error("미션 선택 중 예상치 못한 오류: userId={}, missionId={}, groupId={}",
                    userId, requestDto.getMissionId(), requestDto.getGroupId(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "서버 오류입니다."));
        }
    }
}