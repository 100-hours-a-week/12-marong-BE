package com.ktb.marong.dto.request.feed;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PostRequestDto {

    @NotNull(message = "미션 ID는 필수입니다.")
    private Long missionId;

    @NotBlank(message = "마니또 이름은 필수입니다.")
    private String manittoName;

    @NotBlank(message = "게시글 내용은 필수입니다.")
    @Size(max = 300, message = "게시글 내용은 최대 300자까지 입력 가능합니다.")
    private String content;

    // 이미지는 MultipartFile로 별도 처리
}