package com.ktb.marong.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.assertj.core.api.Assertions.*;

/**
 * User 엔티티 단위 테스트
 */
@Tag("unit")
class UserTest {

    @Test
    @DisplayName("User 빌더로 정상 생성 테스트")
    void createUserWithBuilder() {
        // given
        String email = "test@example.com";
        String providerId = "kakao_123456";
        String nickname = "테스트유저";
        String providerName = "kakao";
        String profileImageUrl = "https://example.com/profile.jpg";

        // when
        User user = User.builder()
                .email(email)
                .providerId(providerId)
                .nickname(nickname)
                .providerName(providerName)
                .profileImageUrl(profileImageUrl)
                .build();

        // then
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getProviderId()).isEqualTo(providerId);
        assertThat(user.getNickname()).isEqualTo(nickname);
        assertThat(user.getProviderName()).isEqualTo(providerName);
        assertThat(user.getProfileImageUrl()).isEqualTo(profileImageUrl);
        assertThat(user.getStatus()).isEqualTo("active");
        assertThat(user.getHasCompletedSurvey()).isFalse();
    }

    @Test
    @DisplayName("필수 필드만으로 User 생성 테스트")
    void createUserWithRequiredFields() {
        // given
        String email = "test@example.com";
        String providerId = "kakao_123456";
        String nickname = "테스트유저";

        // when
        User user = User.builder()
                .email(email)
                .providerId(providerId)
                .nickname(nickname)
                .build();

        // then
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getProviderId()).isEqualTo(providerId);
        assertThat(user.getNickname()).isEqualTo(nickname);
        assertThat(user.getProviderName()).isNull();
        assertThat(user.getProfileImageUrl()).isNull();
        assertThat(user.getStatus()).isEqualTo("active");
        assertThat(user.getHasCompletedSurvey()).isFalse();
    }

    @Test
    @DisplayName("설문 완료 처리 메서드 테스트")
    void completeInitialSurvey() {
        // given
        User user = User.builder()
                .email("test@example.com")
                .providerId("kakao_123456")
                .nickname("테스트유저")
                .build();

        // when
        user.completeInitialSurvey();

        // then
        assertThat(user.getHasCompletedSurvey()).isTrue();
    }

    @Test
    @DisplayName("프로필 업데이트 메서드 테스트")
    void updateProfile() {
        // given
        User user = User.builder()
                .email("test@example.com")
                .providerId("kakao_123456")
                .nickname("이전닉네임")
                .profileImageUrl("https://old-image.com/profile.jpg")
                .build();

        String newNickname = "새로운닉네임";
        String newProfileImageUrl = "https://new-image.com/profile.jpg";

        // when
        user.updateProfile(newNickname, newProfileImageUrl);

        // then
        assertThat(user.getNickname()).isEqualTo(newNickname);
        assertThat(user.getProfileImageUrl()).isEqualTo(newProfileImageUrl);
    }

    @Test
    @DisplayName("프로필 업데이트 시 null 값 처리 테스트")
    void updateProfileWithNullValues() {
        // given
        User user = User.builder()
                .email("test@example.com")
                .providerId("kakao_123456")
                .nickname("이전닉네임")
                .profileImageUrl("https://old-image.com/profile.jpg")
                .build();

        // when
        user.updateProfile(null, null);

        // then
        assertThat(user.getNickname()).isNull();
        assertThat(user.getProfileImageUrl()).isNull();
    }

    @Test
    @DisplayName("소프트 삭제 메서드 테스트")
    void softDelete() {
        // given
        User user = User.builder()
                .email("test@example.com")
                .providerId("kakao_123456")
                .nickname("테스트유저")
                .build();

        // when
        user.softDelete();

        // then
        assertThat(user.getStatus()).isEqualTo("deleted");
        assertThat(user.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 삭제된 사용자 재삭제 테스트")
    void softDeleteAlreadyDeletedUser() {
        // given
        User user = User.builder()
                .email("test@example.com")
                .providerId("kakao_123456")
                .nickname("테스트유저")
                .build();

        user.softDelete(); // 첫 번째 삭제
        var firstDeletedAt = user.getDeletedAt();

        // when
        user.softDelete(); // 두 번째 삭제

        // then
        assertThat(user.getStatus()).isEqualTo("deleted");
        assertThat(user.getDeletedAt()).isNotEqualTo(firstDeletedAt);
    }
}