package com.ktb.marong.controller;

import com.ktb.marong.dto.request.group.CreateGroupRequestDto;
import com.ktb.marong.dto.request.group.JoinGroupRequestDto;
import com.ktb.marong.dto.request.group.UpdateGroupProfileRequestDto;
import com.ktb.marong.dto.response.common.ApiResponse;
import com.ktb.marong.dto.response.group.CreateGroupResponseDto;
import com.ktb.marong.dto.response.group.GroupDetailResponseDto;
import com.ktb.marong.dto.response.group.GroupResponseDto;
import com.ktb.marong.dto.response.group.JoinGroupResponseDto;
import com.ktb.marong.security.CurrentUser;
import com.ktb.marong.service.group.GroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/groups")
public class GroupController {

    private final GroupService groupService;

    /**
     * 그룹 생성
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createGroup(
            @CurrentUser Long userId,
            @RequestParam("groupName") String groupName,
            @RequestParam("description") String description,
            @RequestParam("inviteCode") String inviteCode,
            @RequestParam(value = "groupImage", required = false) MultipartFile groupImage,
            @RequestParam("groupUserNickname") String groupUserNickname,
            @RequestParam(value = "groupUserProfileImage", required = false) MultipartFile groupUserProfileImage) {

        log.info("그룹 생성 요청: userId={}, groupName={}", userId, groupName);

        CreateGroupRequestDto requestDto = new CreateGroupRequestDto(
                groupName, description, inviteCode, groupUserNickname, null);

        CreateGroupResponseDto response = groupService.createGroup(userId, requestDto, groupImage, groupUserProfileImage);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        response,
                        "group_created",
                        null
                ));
    }

    /**
     * 그룹 가입
     */
    @PostMapping(value = "/{groupId}/join", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> joinGroup(
            @CurrentUser Long userId,
            @PathVariable Long groupId,
            @RequestParam("inviteCode") String inviteCode,
            @RequestParam("groupUserNickname") String groupUserNickname,
            @RequestParam(value = "groupUserProfileImage", required = false) MultipartFile groupUserProfileImage) {

        log.info("그룹 가입 요청: userId={}, groupId={}, inviteCode={}", userId, groupId, inviteCode);

        JoinGroupRequestDto requestDto = new JoinGroupRequestDto(inviteCode, groupUserNickname, null);

        JoinGroupResponseDto response = groupService.joinGroup(userId, groupId, requestDto, groupUserProfileImage);

        return ResponseEntity.ok(ApiResponse.success(
                response,
                "group_joined",
                null
        ));
    }

    /**
     * 내가 속한 그룹 목록 조회
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMyGroups(@CurrentUser Long userId) {
        log.info("내 그룹 목록 조회: userId={}", userId);

        List<GroupResponseDto> response = groupService.getMyGroups(userId);

        return ResponseEntity.ok(ApiResponse.success(
                response,
                "groups_retrieved",
                null
        ));
    }

    /**
     * 그룹 프로필 정보 업데이트
     */
    @PutMapping(value = "/{groupId}/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateGroupProfile(
            @CurrentUser Long userId,
            @PathVariable Long groupId,
            @RequestParam("groupUserNickname") String groupUserNickname,
            @RequestParam(value = "groupUserProfileImage", required = false) MultipartFile groupUserProfileImage) {

        log.info("그룹 프로필 업데이트 요청: userId={}, groupId={}", userId, groupId);

        UpdateGroupProfileRequestDto requestDto = new UpdateGroupProfileRequestDto(groupUserNickname);

        groupService.updateGroupProfile(userId, groupId, requestDto, groupUserProfileImage);

        return ResponseEntity.ok(ApiResponse.success(
                null,
                "group_profile_updated",
                null
        ));
    }

    /**
     * 특정 그룹 상세 정보 조회 (멤버 수 제한 정보 포함)
     */
    @GetMapping("/{groupId}")
    public ResponseEntity<?> getGroupDetail(@CurrentUser Long userId, @PathVariable Long groupId) {
        log.info("그룹 상세 정보 조회: userId={}, groupId={}", userId, groupId);

        GroupDetailResponseDto response = groupService.getGroupDetail(userId, groupId);

        return ResponseEntity.ok(ApiResponse.success(
                response,
                "group_detail_retrieved",
                null
        ));
    }
}