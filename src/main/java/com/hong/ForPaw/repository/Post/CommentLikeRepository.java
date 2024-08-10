package com.hong.ForPaw.repository.Post;

import com.hong.ForPaw.domain.Post.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    Optional<CommentLike> findByUserIdAndCommentId(Long userId, Long commentId);

    @Query("SELECT cl.comment.id FROM CommentLike cl WHERE cl.user.id = :userId")
    List<Long> findLikeCommentIdListByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM CommentLike cl WHERE cl.comment.id IN (SELECT c.id FROM Comment c WHERE c.post.id = :postId)")
    void deleteAllByPostId(@Param("postId") Long postId);

    void deleteAllByCommentId(Long commentId);

    @Modifying
    @Query("DELETE FROM CommentLike cl WHERE cl.comment.id IN (SELECT c.id FROM Comment c WHERE c.post.id IN (SELECT p.id FROM Post p WHERE p.group.id = :groupId))")
    void deleteByGroupId(@Param("groupId") Long groupId);

    @Modifying
    @Query("DELETE FROM CommentLike cl WHERE cl.user.id = :userId")
    void deleteByUserId(Long userId);
}