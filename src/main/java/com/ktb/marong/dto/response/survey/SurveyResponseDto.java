package com.ktb.marong.dto.response.survey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurveyResponseDto {

    private Integer eiScore;
    private Integer snScore;
    private Integer tfScore;
    private Integer jpScore;
    private List<String> hobbies;
    private List<String> likedFoods;
    private List<String> dislikedFoods;
}