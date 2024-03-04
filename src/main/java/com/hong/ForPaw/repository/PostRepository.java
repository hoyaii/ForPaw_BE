package com.hong.ForPaw.repository;

import com.hong.ForPaw.domain.Post.Post;
import com.hong.ForPaw.domain.Post.Type;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByType(Type type, Pageable pageable);

    boolean existsById(Long id);

    @Query("SELECT (COUNT(p) > 0) FROM Post p WHERE p.id = :postId AND p.user.id = :userId")
    boolean isOwnPost(@Param("postId") Long postId, @Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Post p SET p.likeNum = p.likeNum + 1 WHERE p.id = :postId")
    void incrementLikeCountById(@Param("postId") Long postId);

    @Modifying
    @Query("UPDATE Post p SET p.likeNum = p.likeNum - 1 WHERE p.id = :postId AND p.likeNum > 0")
    void decrementLikeCountById(@Param("postId") Long postId);
}