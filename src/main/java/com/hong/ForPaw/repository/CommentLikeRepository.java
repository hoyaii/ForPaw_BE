package com.hong.ForPaw.repository;

import com.hong.ForPaw.domain.Post.CommentLike;
import com.hong.ForPaw.domain.Post.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    Optional<CommentLike> findByUserIdAndCommentId(Long userId, Long commentId);
}
