package com.hong.forapw.repository.post;

import com.hong.forapw.domain.post.Comment;
import com.hong.forapw.domain.user.User;
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

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT c FROM Comment c WHERE c.id = :commentId AND c.removedAt IS NULL")
    Optional<Comment> findByIdWithUser(@Param("commentId") Long commentId);

    @Query("SELECT u FROM Comment c " +
            "JOIN c.user u " +
            "WHERE c.id = :commentId AND c.removedAt IS NULL")
    Optional<User> findUserById(@Param("commentId") Long commentId);

    @Query("SELECT u.id FROM Comment c " +
            "JOIN c.user u " +
            "WHERE c.id = :commentId AND c.removedAt IS NULL")
    Optional<Long> findUserIdById(@Param("commentId") Long commentId);

    // 모든 댓글과 대댓글을 한 번에 가져오기
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId")
    @EntityGraph(attributePaths = {"user", "parent"})
    List<Comment> findByPostIdWithUserAndParentAndRemoved(@Param("postId") Long postId);

    @EntityGraph(attributePaths = {"post"})
    @Query("SELECT c FROM Comment c " +
            "JOIN c.user u " +
            "WHERE u.id = :userId AND c.removedAt IS NULL")
    Page<Comment> findByUserIdWithPost(@Param("userId") Long userId, Pageable pageable);

    // date 이후의 댓글이 존재 => 마지막 대댓글이 아니다
    @Query("SELECT COUNT(c) > 0 FROM Comment c " +
            "JOIN c.parent p " +
            "WHERE p.id = :parentId AND c.createdDate > :date AND c.removedAt IS NULL")
    boolean existsByParentIdAndDateAfter(Long parentId, LocalDateTime date);

    @Query("SELECT COUNT(cl) FROM CommentLike cl WHERE cl.comment.id = :commentId")
    Long countLikesByCommentId(@Param("commentId") Long commentId);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.createdDate >= :date AND c.removedAt IS NULL")
    Long countALlWithinDate(LocalDateTime date);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.user.id = :userId AND c.removedAt IS NULL")
    Long countByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Comment c SET c.removedAt = NOW() WHERE c.post.id= :postId")
    void deleteByPostId(Long postId);

    @Modifying
    @Query("UPDATE Comment c SET c.removedAt = NOW() WHERE c.id= :commentId")
    void deleteById(@Param("commentId") Long commentId);

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.post.id IN (SELECT p.id FROM Post p WHERE p.group.id = :groupId ) AND c.parent IS NOT NULL")
    void hardDeleteChildByGroupId(@Param("groupId") Long groupId);

    // 자식 댓글 => 부모 댓글 순으로 삭제해야 Foregin 키 제약에 걸리지 않음
    @Modifying
    @Query("DELETE FROM Comment c WHERE c.post.id IN (SELECT p.id FROM Post p WHERE p.group.id = :groupId ) AND c.parent IS NULL")
    void hardDeleteParentByGroupId(@Param("groupId") Long groupId);
}