package com.ktb.marong.dto.request.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGroupProfileRequestDto {

    @NotBlank(message = "그룹 내 사용자 닉네임은 필수입니다.")
    @Size(max = 20, message = "그룹 내 사용자 닉네임은 최대 20자까지 입력 가능합니다.")
    private String groupUserNickname;
}