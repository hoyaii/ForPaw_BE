package com.hong.ForPaw.repository.Post;

import com.hong.ForPaw.domain.Post.Comment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Override
    @EntityGraph(attributePaths = {"user", "post"})
    Optional<Comment> findById(Long id);

    @Query("SELECT c.user.id FROM Comment c WHERE c.id = :commentId")
    Optional<Long> findUserIdByCommentId(@Param("commentId") Long commentId);

    // 모든 댓글과 대댓글을 한 번에 가져오기
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.user WHERE c.post.id = :postId ORDER BY c.parent.id ASC NULLS FIRST, c.createdDate ASC")
    List<Comment> findAllCommentsWithRepliesByPostId(@Param("postId") Long postId);

    boolean existsById(Long id);

    @Modifying
    @Query("UPDATE Comment c SET c.content = :content WHERE c.id = :commentId")
    void updateCommentContent(@Param("commentId") Long commentId, @Param("content") String content);

    @Query("SELECT (COUNT(c) > 0) FROM Comment c WHERE c.id = :commentId AND c.user.id = :userId")
    boolean isOwnComment(@Param("commentId") Long commentId, @Param("userId") Long userId);

    void deleteAllByPostId(Long postId);
}