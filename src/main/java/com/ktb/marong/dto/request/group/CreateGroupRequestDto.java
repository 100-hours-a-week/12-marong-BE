package com.ktb.marong.dto.request.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupRequestDto {

    @NotBlank(message = "그룹 이름은 필수입니다.")
    @Size(max = 30, message = "그룹 이름은 최대 30자까지 입력 가능합니다.")
    private String groupName;

    @Size(max = 200, message = "그룹 설명은 최대 200자까지 입력 가능합니다.")
    private String description;

    @NotBlank(message = "초대 코드는 필수입니다.")
    @Pattern(regexp = "^[0-9]{6}$", message = "초대 코드는 6자리 숫자여야 합니다.")
    private String inviteCode;

    @NotBlank(message = "그룹 내 사용자 닉네임은 필수입니다.")
    @Size(max = 20, message = "그룹 내 사용자 닉네임은 최대 20자까지 입력 가능합니다.")
    private String groupUserNickname;

    private String groupUserProfileImageUrl; // 선택사항
}