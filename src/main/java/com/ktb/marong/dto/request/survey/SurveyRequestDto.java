package com.ktb.marong.dto.request.survey;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SurveyRequestDto {

    @NotNull(message = "ei_score는 필수입니다.")
    @Min(value = 0, message = "ei_score는 0 이상이어야 합니다.")
    @Max(value = 100, message = "ei_score는 100 이하이어야 합니다.")
    private Integer eiScore;

    @NotNull(message = "sn_score는 필수입니다.")
    @Min(value = 0, message = "sn_score는 0 이상이어야 합니다.")
    @Max(value = 100, message = "sn_score는 100 이하이어야 합니다.")
    private Integer snScore;

    @NotNull(message = "tf_score는 필수입니다.")
    @Min(value = 0, message = "tf_score는 0 이상이어야 합니다.")
    @Max(value = 100, message = "tf_score는 100 이하이어야 합니다.")
    private Integer tfScore;

    @NotNull(message = "jp_score는 필수입니다.")
    @Min(value = 0, message = "jp_score는 0 이상이어야 합니다.")
    @Max(value = 100, message = "jp_score는 100 이하이어야 합니다.")
    private Integer jpScore;

    @NotEmpty(message = "취미는 최소 1개 이상이어야 합니다.")
    private List<String> hobbies;

    @NotEmpty(message = "좋아하는 음식 목록은 필수입니다.")
    private List<String> likedFoods;

    @NotEmpty(message = "싫어하는 음식 목록은 필수입니다.")
    private List<String> dislikedFoods;
}