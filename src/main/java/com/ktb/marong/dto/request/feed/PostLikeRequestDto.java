package com.ktb.marong.dto.request.feed;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PostLikeRequestDto {

    @NotNull(message = "cancel 필드는 필수입니다.")
    private Boolean cancel;
}