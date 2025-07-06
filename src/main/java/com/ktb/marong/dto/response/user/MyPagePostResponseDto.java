package com.ktb.marong.dto.response.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyPagePostResponseDto {
    private GroupInfo groupInfo;
    private List<PostInfo> posts;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupInfo {
        private Long groupId;
        private String groupName;
        private String groupImageUrl;
        private int postCount;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostInfo {
        private Long feedId;
        private String author;
        private String authorProfileImageUrl;
        private String missionTitle;
        private String manitteeName;
        private String content;
        private int likes;
        private LocalDateTime createdAt;
        private String imageUrl;
        private Integer week;

        @JsonProperty("isLiked")
        private boolean liked;

        // MBTI 업데이트 정보
        private MbtiUpdateInfo mbtiUpdate;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MbtiUpdateInfo {
        private String changedMbtiType;
        private String changeReason;
        private Integer previousScore;
        private Integer currentScore;
        private LocalDateTime updatedAt;
    }
}
