package com.ktb.marong.repository;

import com.ktb.marong.domain.feed.Post;
import com.ktb.marong.domain.feed.PostLike;
import com.ktb.marong.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    Optional<PostLike> findByUserAndPost(User user, Post post);

    boolean existsByUserAndPost(User user, Post post);

    @Query("SELECT COUNT(pl) FROM PostLike pl WHERE pl.post.id = :postId")
    int countByPostId(@Param("postId") Long postId);

    void deleteByUserAndPost(User user, Post post);
}