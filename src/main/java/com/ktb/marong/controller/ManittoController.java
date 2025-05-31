package com.ktb.marong.controller;

import com.ktb.marong.domain.mission.UserMission;
import com.ktb.marong.dto.response.common.ApiResponse;
import com.ktb.marong.dto.response.manitto.ManittoInfoResponseDto;
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
     * 현재 마니또가 담당하는 마니띠 정보 조회 (그룹 ID 파라미터 추가)
     */
    @GetMapping
    public ResponseEntity<?> getCurrentManitto(
            @CurrentUser Long userId,
            @RequestParam(required = false) Long groupId) {

        // groupId가 없으면 기본 그룹(1) 사용 (MVP 호환성)
        Long targetGroupId = (groupId != null) ? groupId : 1L;

        log.info("마니또가 담당하는 마니띠 정보 요청: userId={}, groupId={}", userId, targetGroupId);

        ManittoInfoResponseDto response = manittoService.getCurrentManittoInfo(userId, targetGroupId);

        return ResponseEntity.ok(ApiResponse.success(
                response,
                "manitto_info_retrieved",
                null
        ));
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

        var response = manittoService.getMissionStatus(userId, targetGroupId);

        return ResponseEntity.ok(ApiResponse.success(
                response,
                "mission_status_retrieved",
                null
        ));
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

        UserMission newMission = manittoService.assignNewMission(userId, targetGroupId);

        return ResponseEntity.ok(ApiResponse.success(
                Map.of(
                        "missionId", newMission.getMission().getId(),
                        "title", newMission.getMission().getTitle(),
                        "description", newMission.getMission().getDescription(),
                        "groupId", targetGroupId
                ),
                "mission_assigned",
                null
        ));
    }
}