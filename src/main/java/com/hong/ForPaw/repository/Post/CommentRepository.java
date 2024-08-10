package com.hong.ForPaw.repository.Post;

import com.hong.ForPaw.domain.Post.Comment;
import com.hong.ForPaw.domain.Post.Post;
import com.hong.ForPaw.domain.Post.PostType;
import com.hong.ForPaw.domain.User.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c WHERE c.id = :id AND c.removedAt IS NULL")
    Optional<Comment> findById(@Param("id") Long id);

    @Query("SELECT c FROM Comment c WHERE c.id = :commentId AND c.removedAt IS NULL")
    @EntityGraph(attributePaths = {"user"})
    Optional<Comment> findByIdWithUser(@Param("commentId") Long commentId);

    @Query("SELECT c.user FROM Comment c WHERE c.id = :commentId AND c.removedAt IS NULL")
    Optional<User> findUserById(@Param("commentId") Long commentId);

    @Query("SELECT c.user.id FROM Comment c WHERE c.id = :commentId AND c.removedAt IS NULL")
    Optional<Long> findWriterIdById(@Param("commentId") Long commentId);

    // 모든 댓글과 대댓글을 한 번에 가져오기
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId")
    @EntityGraph(attributePaths = {"user", "parent"})
    List<Comment> findByPostIdWithUserAndParentAndRemoved(@Param("postId") Long postId);

    @EntityGraph(attributePaths = {"post"})
    @Query("SELECT c FROM Comment c WHERE c.user.id = :userId AND c.removedAt IS NULL")
    Page<Comment> findByUserIdWithPost(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.createdDate >= :date AND c.removedAt IS NULL")
    Long countALlWithinDate(LocalDateTime date);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.user.id = :userId AND c.removedAt IS NULL")
    Long countByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Comment c SET c.removedAt = NOW() WHERE c.post.id= :postId")
    void deleteByPostId(Long postId);

    @Modifying
    @Query("UPDATE Comment c SET c.removedAt = NOW() WHERE c.id= :id")
    void deleteById(@Param("id") Long id);

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.post.id IN (SELECT p.id FROM Post p WHERE p.group.id = :groupId )")
    void hardDeleteByGroupId(@Param("groupId") Long groupId);

    // date 이후의 댓글이 존재 => 마지막 대댓글이 아니다
    @Query("SELECT COUNT(c) > 0 FROM Comment c WHERE c.parent.id = :parentId AND c.createdDate > :date AND c.removedAt IS NULL")
    boolean existsByParentIdAndDateAfter(Long parentId, LocalDateTime date);
}