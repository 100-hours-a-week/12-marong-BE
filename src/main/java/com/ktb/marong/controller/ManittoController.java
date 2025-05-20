package com.ktb.marong.controller;

import com.ktb.marong.domain.mission.UserMission;
import com.ktb.marong.dto.response.common.ApiResponse;
import com.ktb.marong.dto.response.manitto.ManittoInfoResponseDto;
import com.ktb.marong.security.CurrentUser;
import com.ktb.marong.service.manitto.ManittoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/manitto")
public class ManittoController {

    private final ManittoService manittoService;

    /**
     * 현재 마니또가 담당하는 마니띠 정보 조회
     * MVP에서는 모든 사용자가 기본 그룹(ID: 1)에 속한다고 가정
     */
    @GetMapping
    public ResponseEntity<?> getCurrentManitto(@CurrentUser Long userId) {
        log.info("마니또가 담당하는 마니띠 정보 요청: userId={}", userId);

        ManittoInfoResponseDto response = manittoService.getCurrentManittoInfo(userId);

        return ResponseEntity.ok(ApiResponse.success(
                response,
                "manitto_info_retrieved",
                null
        ));
    }

    /**
     * 마니또 미션 상태 조회
     */
    @GetMapping("/missions")
    public ResponseEntity<?> getMissionStatus(@CurrentUser Long userId) {
        log.info("미션 상태 요청: userId={}", userId);

        var response = manittoService.getMissionStatus(userId);

        return ResponseEntity.ok(ApiResponse.success(
                response,
                "mission_status_retrieved",
                null
        ));
    }

    /**
     * 새 미션 할당
     */
    @PostMapping("/missions/assign")
    public ResponseEntity<?> assignNewMission(@CurrentUser Long userId) {
        log.info("새 미션 할당 요청: userId={}", userId);

        UserMission newMission = manittoService.assignNewMission(userId);

        return ResponseEntity.ok(ApiResponse.success(
                Map.of(
                        "missionId", newMission.getMission().getId(),
                        "title", newMission.getMission().getTitle(),
                        "description", newMission.getMission().getDescription()
                ),
                "mission_assigned",
                null
        ));
    }
}