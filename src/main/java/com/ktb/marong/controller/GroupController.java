package com.ktb.marong.controller;

import com.ktb.marong.common.util.GroupNicknameValidator;
import com.ktb.marong.common.util.GroupValidator;
import com.ktb.marong.dto.request.group.CreateGroupRequestDto;
import com.ktb.marong.dto.request.group.JoinGroupRequestDto;
import com.ktb.marong.dto.request.group.UpdateGroupProfileRequestDto;
import com.ktb.marong.dto.response.common.ApiResponse;
import com.ktb.marong.dto.response.group.*;
import com.ktb.marong.exception.CustomException;
import com.ktb.marong.repository.GroupRepository;
import com.ktb.marong.repository.UserGroupRepository;
import com.ktb.marong.security.CurrentUser;
import com.ktb.marong.service.group.GroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/groups")
public class GroupController {

    private final GroupService groupService;
    private final UserGroupRepository userGroupRepository;
    private final GroupRepository groupRepository;

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

    /**
     * 그룹 이름 중복 체크 API
     */
    @GetMapping("/name/check")
    public ResponseEntity<?> checkGroupName(@RequestParam String groupName) {
        log.info("그룹 이름 중복 체크 요청: groupName={}", groupName);

        try {
            // 그룹 이름 형식 검증
            GroupValidator.validateGroupName(groupName);
            String normalizedName = GroupValidator.normalizeGroupName(groupName);

            boolean isDuplicated = groupRepository.existsByNormalizedName(normalizedName);

            Map<String, Object> response = new HashMap<>();
            response.put("available", !isDuplicated);
            response.put("groupName", normalizedName);

            log.info("그룹 이름 중복 체크 결과: groupName={}, available={}",
                    normalizedName, !isDuplicated);

            return ResponseEntity.ok(ApiResponse.success(
                    response,
                    isDuplicated ? "group_name_duplicated" : "group_name_available",
                    null
            ));

        } catch (CustomException e) {
            log.warn("그룹 이름 형식 오류: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getErrorCode().name(), e.getMessage()));
        }
    }

    /**
     * 그룹 내 닉네임 중복 체크 API
     */
    @GetMapping("/{groupId}/nickname/check")
    public ResponseEntity<?> checkNickname(
            @CurrentUser Long userId,
            @PathVariable Long groupId,
            @RequestParam String nickname) {

        log.info("닉네임 중복 체크 요청: userId={}, groupId={}, nickname={}", userId, groupId, nickname);

        try {
            // 닉네임 형식 검증
            GroupNicknameValidator.validateNicknameFormat(nickname);
            String normalizedNickname = GroupNicknameValidator.normalizeNickname(nickname);

            // 사용자가 해당 그룹에 이미 닉네임을 설정했는지 확인
            boolean hasExistingNickname = groupService.hasGroupNickname(userId, groupId);

            boolean isDuplicated;
            if (hasExistingNickname) {
                // 기존 닉네임이 있는 경우 자신 제외하고 중복 체크
                isDuplicated = userGroupRepository.existsByGroupIdAndGroupUserNicknameExcludingUser(
                        groupId, normalizedNickname, userId);
            } else {
                // 첫 닉네임 설정인 경우 전체 중복 체크
                isDuplicated = userGroupRepository.existsByGroupIdAndGroupUserNickname(
                        groupId, normalizedNickname);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("available", !isDuplicated);
            response.put("nickname", normalizedNickname);
            response.put("isFirstTime", !hasExistingNickname);

            log.info("닉네임 중복 체크 결과: groupId={}, nickname={}, available={}, isFirstTime={}",
                    groupId, normalizedNickname, !isDuplicated, !hasExistingNickname);

            return ResponseEntity.ok(ApiResponse.success(
                    response,
                    isDuplicated ? "nickname_duplicated" : "nickname_available",
                    null
            ));

        } catch (CustomException e) {
            log.warn("닉네임 형식 오류: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getErrorCode().name(), e.getMessage()));
        }
    }

    /**
     * 그룹 내 사용 중인 닉네임 목록 조회 API
     */
    @GetMapping("/{groupId}/nicknames")
    public ResponseEntity<?> getUsedNicknames(
            @CurrentUser Long userId,
            @PathVariable Long groupId) {

        log.info("그룹 내 사용 중인 닉네임 목록 조회: userId={}, groupId={}", userId, groupId);

        try {
            List<String> usedNicknames = groupService.getUsedNicknames(groupId);

            Map<String, Object> response = new HashMap<>();
            response.put("nicknames", usedNicknames);
            response.put("count", usedNicknames.size());

            return ResponseEntity.ok(ApiResponse.success(
                    response,
                    "nicknames_retrieved",
                    null
            ));

        } catch (Exception e) {
            log.error("닉네임 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "서버 오류입니다."));
        }
    }

    /**
     * 사용자의 특정 그룹 내 닉네임 설정 여부 확인 API
     */
    @GetMapping("/{groupId}/nickname/status")
    public ResponseEntity<?> checkNicknameStatus(
            @CurrentUser Long userId,
            @PathVariable Long groupId) {

        log.info("그룹 내 닉네임 설정 여부 확인 요청: userId={}, groupId={}", userId, groupId);

        try {
            boolean hasNickname = groupService.hasGroupNickname(userId, groupId);

            Map<String, Object> response = new HashMap<>();
            response.put("hasNickname", hasNickname);
            response.put("required", !hasNickname);

            return ResponseEntity.ok(ApiResponse.success(
                    response,
                    hasNickname ? "nickname_already_set" : "nickname_required",
                    null
            ));

        } catch (Exception e) {
            log.error("닉네임 상태 확인 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "서버 오류입니다."));
        }
    }

    /**
     * 전체 그룹 목록 조회 (공개 그룹 목록)
     * 그룹 가입을 위해 그룹 ID를 알기 위한 API
     */
    @GetMapping("/public")
    public ResponseEntity<?> getAllPublicGroups(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        log.info("전체 그룹 목록 조회 요청: page={}, pageSize={}", page, pageSize);

        try {
            // 페이지 크기 제한 (최대 50개)
            if (pageSize > 50) {
                pageSize = 50;
            }

            Page<PublicGroupResponseDto> groupPage = groupService.getAllPublicGroups(page, pageSize);

            Map<String, Object> response = new HashMap<>();
            response.put("groups", groupPage.getContent());
            response.put("currentPage", groupPage.getNumber() + 1);
            response.put("pageSize", groupPage.getSize());
            response.put("totalElements", groupPage.getTotalElements());
            response.put("totalPages", groupPage.getTotalPages());
            response.put("isFirst", groupPage.isFirst());
            response.put("isLast", groupPage.isLast());

            return ResponseEntity.ok(ApiResponse.success(
                    response,
                    "public_groups_retrieved",
                    null
            ));

        } catch (Exception e) {
            log.error("전체 그룹 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "서버 오류입니다."));
        }
    }
}