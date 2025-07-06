package com.ktb.marong.dto.request.recommendation;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PlaceLikeRequestDto {
    private Boolean cancel; // true: 좋아요 취소, false: 좋아요 등록
}