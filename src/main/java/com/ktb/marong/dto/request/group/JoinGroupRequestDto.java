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
public class JoinGroupRequestDto {

    @NotBlank(message = "초대 코드는 필수입니다.")
    @Pattern(regexp = "^[A-Za-z0-9]{6}$", message = "초대 코드는 영어와 숫자를 포함한 6자리여야 합니다.")
    private String inviteCode;

    @NotBlank(message = "그룹 내 사용자 닉네임은 필수입니다.")
    @Size(max = 20, message = "그룹 내 사용자 닉네임은 최대 20자까지 입력 가능합니다.")
    private String groupUserNickname;

    private String groupUserProfileImageUrl; // 선택사항
}