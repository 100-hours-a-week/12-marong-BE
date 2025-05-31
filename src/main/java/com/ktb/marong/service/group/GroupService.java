package com.ktb.marong.service.group;

import com.ktb.marong.common.util.GroupNicknameValidator;
import com.ktb.marong.common.util.InviteCodeValidator;
import com.ktb.marong.domain.group.Group;
import com.ktb.marong.domain.group.UserGroup;
import com.ktb.marong.domain.user.User;
import com.ktb.marong.dto.request.group.CreateGroupRequestDto;
import com.ktb.marong.dto.request.group.JoinGroupRequestDto;
import com.ktb.marong.dto.request.group.UpdateGroupProfileRequestDto;
import com.ktb.marong.dto.response.group.*;
import com.ktb.marong.exception.CustomException;
import com.ktb.marong.exception.ErrorCode;
import com.ktb.marong.repository.GroupRepository;
import com.ktb.marong.repository.UserGroupRepository;
import com.ktb.marong.repository.UserRepository;
import com.ktb.marong.service.file.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserRepository userRepository;
    private final FileUploadService fileUploadService;

    private static final int MAX_GROUPS_PER_USER = 4;
    private static final int MAX_MEMBERS_PER_GROUP = 150;

    /**
     * 그룹 생성
     */
    @Transactional
    public CreateGroupResponseDto createGroup(Long userId, CreateGroupRequestDto requestDto,
                                              MultipartFile groupImage, MultipartFile groupUserProfileImage) {
        log.info("그룹 생성 요청: userId={}, groupName={}, inviteCode={}, nickname={}",
                userId, requestDto.getGroupName(), requestDto.getInviteCode(), requestDto.getGroupUserNickname());

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 그룹 개수 제한 체크
        checkGroupLimit(userId);

        // 초대 코드 유효성 검증
        InviteCodeValidator.validateInviteCode(requestDto.getInviteCode());
        String normalizedInviteCode = InviteCodeValidator.normalizeInviteCode(requestDto.getInviteCode());

        // 닉네임 형식 검증 및 정규화
        GroupNicknameValidator.validateNicknameFormat(requestDto.getGroupUserNickname());
        String normalizedNickname = GroupNicknameValidator.normalizeNickname(requestDto.getGroupUserNickname());

        // 그룹 이름 중복 체크
        if (groupRepository.existsByName(requestDto.getGroupName())) {
            throw new CustomException(ErrorCode.GROUP_NAME_DUPLICATED);
        }

        // 초대 코드 중복 체크 (대소문자 구분 안함)
        if (groupRepository.existsByInviteCode(normalizedInviteCode)) {
            throw new CustomException(ErrorCode.INVITE_CODE_DUPLICATED);
        }

        // 그룹 이미지 업로드 처리
        String groupImageUrl = uploadGroupImage(groupImage);

        // 사용자 프로필 이미지 업로드 처리
        String userProfileImageUrl = uploadUserProfileImage(groupUserProfileImage);

        // 그룹 생성
        Group group = Group.builder()
                .name(requestDto.getGroupName())
                .description(requestDto.getDescription())
                .inviteCode(normalizedInviteCode)
                .imageUrl(groupImageUrl)
                .build();

        Group savedGroup = groupRepository.save(group);

        // 생성자를 그룹에 자동 가입 (소유자로 설정)
        UserGroup userGroup = UserGroup.builder()
                .user(user)
                .group(savedGroup)
                .groupUserNickname(normalizedNickname)
                .groupUserProfileImageUrl(userProfileImageUrl)
                .isOwner(true)
                .build();

        userGroupRepository.save(userGroup);

        log.info("그룹 생성 완료: groupId={}, groupName={}, inviteCode={}, nickname={}",
                savedGroup.getId(), savedGroup.getName(), normalizedInviteCode, normalizedNickname);

        return CreateGroupResponseDto.builder()
                .groupId(savedGroup.getId())
                .groupName(savedGroup.getName())
                .inviteCode(normalizedInviteCode)
                .build();
    }

    /**
     * 그룹 가입
     */
    @Transactional
    public JoinGroupResponseDto joinGroup(Long userId, Long groupId, JoinGroupRequestDto requestDto,
                                          MultipartFile groupUserProfileImage) {
        log.info("그룹 가입 요청: userId={}, groupId={}, inviteCode={}, nickname={}",
                userId, groupId, requestDto.getInviteCode(), requestDto.getGroupUserNickname());

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 그룹 개수 제한 체크
        checkGroupLimit(userId);

        // 초대 코드 유효성 검증
        InviteCodeValidator.validateInviteCode(requestDto.getInviteCode());
        String normalizedInviteCode = InviteCodeValidator.normalizeInviteCode(requestDto.getInviteCode());

        // 닉네임 형식 검증 및 정규화
        GroupNicknameValidator.validateNicknameFormat(requestDto.getGroupUserNickname());
        String normalizedNickname = GroupNicknameValidator.normalizeNickname(requestDto.getGroupUserNickname());

        // 그룹 ID로 그룹 조회
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        // 입력된 초대코드와 그룹의 초대코드 비교
        if (!group.getInviteCode().equals(normalizedInviteCode)) {
            throw new CustomException(ErrorCode.INVITE_CODE_MISMATCH);
        }

        // 이미 가입한 그룹인지 확인
        if (userGroupRepository.existsByUserIdAndGroupId(userId, groupId)) {
            throw new CustomException(ErrorCode.ALREADY_JOINED_GROUP);
        }

        // 그룹 멤버 수 제한 체크
        checkGroupMemberLimit(groupId);

        // 닉네임 중복 체크
        checkNicknameDuplication(groupId, normalizedNickname, null);

        // 그룹 내 사용자 프로필 이미지 업로드 처리
        String userProfileImageUrl = uploadUserProfileImage(groupUserProfileImage);

        // 그룹 가입
        UserGroup userGroup = UserGroup.builder()
                .user(user)
                .group(group)
                .groupUserNickname(normalizedNickname)
                .groupUserProfileImageUrl(userProfileImageUrl)
                .isOwner(false)
                .build();

        userGroupRepository.save(userGroup);

        log.info("그룹 가입 완료: userId={}, groupId={}, groupName={}, nickname={}",
                userId, groupId, group.getName(), normalizedNickname);

        return JoinGroupResponseDto.builder()
                .groupId(group.getId())
                .groupName(group.getName())
                .myNickname(normalizedNickname)
                .build();
    }

    /**
     * 내가 속한 그룹 목록 조회 (최근 가입 순으로 정렬)
     */
    @Transactional(readOnly = true)
    public List<GroupResponseDto> getMyGroups(Long userId) {
        log.info("내 그룹 목록 조회: userId={}", userId);

        List<UserGroup> userGroups = userGroupRepository.findByUserIdWithGroup(userId);

        // 최근 가입 순으로 정렬 (joinedAt 기준 내림차순)
        return userGroups.stream()
                .sorted((g1, g2) -> g2.getJoinedAt().compareTo(g1.getJoinedAt()))
                .map(userGroup -> {
                    int memberCount = userGroupRepository.countByGroupId(userGroup.getGroup().getId());
                    return GroupResponseDto.fromUserGroup(userGroup, memberCount);
                })
                .collect(Collectors.toList());
    }

    /**
     * 그룹 프로필 정보 업데이트 (그룹별 닉네임과 프로필 사진 설정)
     */
    @Transactional
    public void updateGroupProfile(Long userId, Long groupId, UpdateGroupProfileRequestDto requestDto,
                                   MultipartFile groupUserProfileImage) {
        log.info("그룹 프로필 업데이트: userId={}, groupId={}, nickname={}",
                userId, groupId, requestDto.getGroupUserNickname());

        // 사용자-그룹 관계 조회
        UserGroup userGroup = userGroupRepository.findByUserIdAndGroupId(userId, groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND, "해당 그룹에 속하지 않은 사용자입니다."));

        // 닉네임 형식 검증 및 정규화
        GroupNicknameValidator.validateNicknameFormat(requestDto.getGroupUserNickname());
        String normalizedNickname = GroupNicknameValidator.normalizeNickname(requestDto.getGroupUserNickname());

        // 기존 닉네임과 동일한지 확인
        if (!normalizedNickname.equals(userGroup.getGroupUserNickname())) {
            // 닉네임이 변경된 경우에만 중복 체크 (자신 제외)
            checkNicknameDuplication(groupId, normalizedNickname, userId);
        }

        String groupUserProfileImageUrl = userGroup.getGroupUserProfileImageUrl();
        if (groupUserProfileImage != null && !groupUserProfileImage.isEmpty()) {
            groupUserProfileImageUrl = uploadUserProfileImage(groupUserProfileImage);
        }

        // 그룹 내 사용자 프로필 정보 업데이트
        userGroup.updateGroupUserProfile(normalizedNickname, groupUserProfileImageUrl);
        userGroupRepository.save(userGroup);

        log.info("그룹 프로필 업데이트 완료: userId={}, groupId={}, nickname={}", userId, groupId, normalizedNickname);
    }

    /**
     * 특정 그룹 상세 정보 조회 (멤버 수 제한 정보 포함)
     */
    @Transactional(readOnly = true)
    public GroupDetailResponseDto getGroupDetail(Long userId, Long groupId) {
        log.info("그룹 상세 정보 조회: userId={}, groupId={}", userId, groupId);

        // 사용자가 해당 그룹에 속해있는지 확인
        UserGroup userGroup = userGroupRepository.findByUserIdAndGroupId(userId, groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND, "해당 그룹에 속하지 않은 사용자입니다."));

        // 현재 멤버 수 조회
        int currentMemberCount = userGroupRepository.countByGroupId(groupId);

        Group group = userGroup.getGroup();

        // 응답 생성
        GroupDetailResponseDto response = GroupDetailResponseDto.builder()
                .groupId(group.getId())
                .groupName(group.getName())
                .description(group.getDescription())
                .groupImageUrl(group.getImageUrl())
                .inviteCode(group.getInviteCode())
                .currentMemberCount(currentMemberCount)
                .maxMemberCount(MAX_MEMBERS_PER_GROUP)
                .myNickname(userGroup.getGroupUserNickname())
                .myProfileImageUrl(userGroup.getGroupUserProfileImageUrl())
                .isOwner(userGroup.getIsOwner())
                .joinedAt(userGroup.getJoinedAt())
                .build();

        log.info("그룹 상세 정보 조회 완료: groupId={}, groupName={}", groupId, group.getName());

        return response;
    }

    /**
     * 그룹 내 닉네임 중복 체크
     */
    private void checkNicknameDuplication(Long groupId, String nickname, Long excludeUserId) {
        boolean isDuplicated;

        if (excludeUserId != null) {
            // 특정 사용자 제외하고 중복 체크 (프로필 수정 시)
            isDuplicated = userGroupRepository.existsByGroupIdAndGroupUserNicknameExcludingUser(
                    groupId, nickname, excludeUserId);
        } else {
            // 전체 중복 체크 (신규 가입 시)
            isDuplicated = userGroupRepository.existsByGroupIdAndGroupUserNickname(groupId, nickname);
        }

        if (isDuplicated) {
            log.warn("그룹 내 닉네임 중복: groupId={}, nickname={}, excludeUserId={}",
                    groupId, nickname, excludeUserId);
            throw new CustomException(ErrorCode.NICKNAME_DUPLICATED_IN_GROUP);
        }
    }

    /**
     * 특정 그룹의 사용 중인 닉네임 목록 조회 (API용)
     */
    @Transactional(readOnly = true)
    public List<String> getUsedNicknames(Long groupId) {
        return userGroupRepository.findAllNicknamesByGroupId(groupId);
    }

    /**
     * 사용자의 특정 그룹 내 닉네임 설정 여부 확인 (내부 메소드)
     */
    @Transactional(readOnly = true)
    public boolean hasGroupNickname(Long userId, Long groupId) {
        Optional<UserGroup> userGroup = userGroupRepository.findByUserIdAndGroupId(userId, groupId);
        return userGroup.map(UserGroup::hasGroupUserNickname).orElse(false);
    }

    /**
     * MVP 호환성: 카카오테크 부트캠프 그룹(ID: 1) 닉네임 설정 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean hasKakaotechGroupNickname(Long userId) {
        return hasGroupNickname(userId, 1L);
    }

    /**
     * 전체 그룹 목록 조회 (페이지네이션, 최신순)
     * 사용자가 그룹 가입 시 그룹 ID를 알기 위해 사용
     */
    @Transactional(readOnly = true)
    public Page<PublicGroupResponseDto> getAllPublicGroups(int page, int pageSize) {
        log.info("전체 그룹 목록 조회 요청: page={}, pageSize={}", page, pageSize);

        // 페이지네이션 설정 (최신순 정렬)
        Pageable pageable = PageRequest.of(page - 1, pageSize);

        // 모든 그룹 조회
        Page<Group> groupPage = groupRepository.findAllOrderByIdDesc(pageable);

        // DTO 변환
        Page<PublicGroupResponseDto> result = groupPage.map(group -> {
            int currentMemberCount = userGroupRepository.countByGroupId(group.getId());
            return PublicGroupResponseDto.fromGroup(group, currentMemberCount, MAX_MEMBERS_PER_GROUP);
        });

        log.info("전체 그룹 목록 조회 완료: totalElements={}, currentPage={}, totalPages={}",
                result.getTotalElements(), result.getNumber() + 1, result.getTotalPages());

        return result;
    }

    // 파일 업로드 관련 메서드들

    private String uploadGroupImage(MultipartFile groupImage) {
        if (groupImage != null && !groupImage.isEmpty()) {
            try {
                return fileUploadService.uploadFile(groupImage, "groups");
            } catch (IOException e) {
                log.error("그룹 이미지 업로드 실패: {}", e.getMessage());
                throw new CustomException(ErrorCode.FILE_UPLOAD_ERROR);
            }
        }
        return null;
    }

    private String uploadUserProfileImage(MultipartFile groupUserProfileImage) {
        if (groupUserProfileImage != null && !groupUserProfileImage.isEmpty()) {
            try {
                return fileUploadService.uploadFile(groupUserProfileImage, "profiles");
            } catch (IOException e) {
                log.error("그룹 내 사용자 프로필 이미지 업로드 실패: {}", e.getMessage());
                throw new CustomException(ErrorCode.FILE_UPLOAD_ERROR);
            }
        }
        return null;
    }

    // 검증 메서드들

    /**
     * 그룹 개수 제한 체크 (사용자당 최대 4개)
     */
    private void checkGroupLimit(Long userId) {
        int currentGroupCount = userGroupRepository.countByUserId(userId);
        if (currentGroupCount >= MAX_GROUPS_PER_USER) {
            throw new CustomException(ErrorCode.MAX_GROUPS_EXCEEDED);
        }
    }

    /**
     * 그룹 멤버 수 제한 체크 (그룹당 최대 150명)
     */
    private void checkGroupMemberLimit(Long groupId) {
        int currentMemberCount = userGroupRepository.countByGroupId(groupId);
        if (currentMemberCount >= MAX_MEMBERS_PER_GROUP) {
            throw new CustomException(ErrorCode.GROUP_MEMBER_LIMIT_EXCEEDED);
        }
    }
}