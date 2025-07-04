package com.ktb.marong.repository;

import com.ktb.marong.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * UserRepository 단위 테스트
 */
@DataJpaTest
@ActiveProfiles("test")
@Tag("unit")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .providerId("kakao_123456")
                .nickname("테스트유저")
                .providerName("kakao")
                .profileImageUrl("https://example.com/profile.jpg")
                .build();
    }

    @Test
    @DisplayName("사용자 저장 및 조회 테스트")
    void saveAndFindUser() {
        // when
        User savedUser = userRepository.save(testUser);
        Optional<User> foundUser = userRepository.findById(savedUser.getId());

        // then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo(testUser.getEmail());
        assertThat(foundUser.get().getProviderId()).isEqualTo(testUser.getProviderId());
        assertThat(foundUser.get().getNickname()).isEqualTo(testUser.getNickname());
    }

    @Test
    @DisplayName("이메일로 사용자 조회 테스트")
    void findByEmail() {
        // given
        userRepository.save(testUser);

        // when
        Optional<User> foundUser = userRepository.findByEmail("test@example.com");

        // then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 사용자 조회 테스트")
    void findByEmailNotFound() {
        // when
        Optional<User> foundUser = userRepository.findByEmail("notfound@example.com");

        // then
        assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("Provider ID로 사용자 조회 테스트")
    void findByProviderId() {
        // given
        userRepository.save(testUser);

        // when
        Optional<User> foundUser = userRepository.findByProviderId("kakao_123456");

        // then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getProviderId()).isEqualTo("kakao_123456");
    }

    @Test
    @DisplayName("이메일 존재 여부 확인 테스트")
    void existsByEmail() {
        // given
        userRepository.save(testUser);

        // when & then
        assertThat(userRepository.existsByEmail("test@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("notfound@example.com")).isFalse();
    }

    @Test
    @DisplayName("Provider ID 존재 여부 확인 테스트")
    void existsByProviderId() {
        // given
        userRepository.save(testUser);

        // when & then
        assertThat(userRepository.existsByProviderId("kakao_123456")).isTrue();
        assertThat(userRepository.existsByProviderId("kakao_999999")).isFalse();
    }

    @Test
    @DisplayName("중복 이메일 저장 시 예외 발생 테스트")
    void saveDuplicateEmail() {
        // given
        userRepository.save(testUser);

        User duplicateUser = User.builder()
                .email("test@example.com") // 중복 이메일
                .providerId("kakao_789012")
                .nickname("다른사용자")
                .build();

        // when & then
        assertThatThrownBy(() -> {
            userRepository.save(duplicateUser);
            entityManager.flush(); // 즉시 DB에 반영하여 제약조건 검증
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("중복 Provider ID 저장 시 예외 발생 테스트")
    void saveDuplicateProviderId() {
        // given
        userRepository.save(testUser);

        User duplicateUser = User.builder()
                .email("different@example.com")
                .providerId("kakao_123456") // 중복 Provider ID
                .nickname("다른사용자")
                .build();

        // when & then
        assertThatThrownBy(() -> {
            userRepository.save(duplicateUser);
            entityManager.flush(); // 즉시 DB에 반영하여 제약조건 검증
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("사용자 삭제 테스트")
    void deleteUser() {
        // given
        User savedUser = userRepository.save(testUser);
        Long userId = savedUser.getId();

        // when
        userRepository.delete(savedUser);

        // then
        Optional<User> deletedUser = userRepository.findById(userId);
        assertThat(deletedUser).isEmpty();
    }

    @Test
    @DisplayName("여러 사용자 저장 및 전체 조회 테스트")
    void saveMultipleUsersAndFindAll() {
        // given
        User user1 = User.builder()
                .email("user1@example.com")
                .providerId("kakao_111111")
                .nickname("사용자1")
                .build();

        User user2 = User.builder()
                .email("user2@example.com")
                .providerId("kakao_222222")
                .nickname("사용자2")
                .build();

        // when
        userRepository.save(user1);
        userRepository.save(user2);

        // then
        assertThat(userRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("설문 완료 상태 업데이트 테스트")
    void updateSurveyCompletionStatus() {
        // given
        User savedUser = userRepository.save(testUser);

        // when
        savedUser.completeInitialSurvey();
        User updatedUser = userRepository.save(savedUser);

        // then
        assertThat(updatedUser.getHasCompletedSurvey()).isTrue();

        // DB에서 다시 조회해서 확인
        Optional<User> reloadedUser = userRepository.findById(updatedUser.getId());
        assertThat(reloadedUser).isPresent();
        assertThat(reloadedUser.get().getHasCompletedSurvey()).isTrue();
    }
}