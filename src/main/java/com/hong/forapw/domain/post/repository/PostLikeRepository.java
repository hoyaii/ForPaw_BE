package com.hong.forapw.domain.post.repository;

import com.hong.forapw.domain.post.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    Optional<PostLike> findByUserIdAndPostId(Long userId, Long postId);

    @Query("SELECT COUNT(pl) > 0 FROM PostLike pl " +
            "JOIN pl.post p " +
            "JOIN pl.user u " +
            "WHERE p.id = :postId AND u.id = :userId")
    boolean existsByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    void deleteAllByPostId(Long postId);

    @Modifying
    @Query("DELETE FROM PostLike pl WHERE pl.post.id IN (SELECT p.id FROM Post p WHERE p.group.id = :groupId)")
    void deleteByGroupId(@Param("groupId") Long groupId);

    @Modifying
    @Query("DELETE FROM PostLike pl WHERE pl.user.id = :userId")
    void deleteByUserId(Long userId);
}
