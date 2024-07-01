package com.hong.ForPaw.repository.Post;

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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p WHERE p.removedAt IS NULL")
    List<Post> findAll();

    @Query("SELECT p FROM Post p WHERE p.createdDate BETWEEN :startOfDay AND :endOfDay AND p.postType = :postType AND p.removedAt IS NULL")
    List<Post> findAllByDate(@Param("startOfDay") LocalDateTime startOfDay,
                             @Param("endOfDay") LocalDateTime endOfDay,
                             @Param("postType") PostType postType);

    @Query("SELECT p FROM Post p WHERE p.id = :id AND p.removedAt IS NULL")
    Optional<Post> findById(@Param("id") Long id);

    @Query("SELECT p.user FROM Post p WHERE p.id = :postId AND p.removedAt IS NULL")
    Optional<User> findUserByPostId(@Param("postId") Long postId);

    @Query("SELECT p.postType FROM Post p WHERE p.id = :postId AND p.removedAt IS NULL")
    Optional<PostType> findPostTypeByPostId(@Param("postId") Long postId);

    @Query("SELECT p.user.id FROM Post p WHERE p.id = :postId AND p.removedAt IS NULL")
    Optional<Long> findUserIdByPostId(@Param("postId") Long postId);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT p FROM Post p WHERE p.group.id = :groupId AND p.removedAt IS NULL")
    Page<Post> findByGroupId(@Param("groupId") Long groupId, Pageable pageable);

    @Query(value = "SELECT * FROM post_tb WHERE MATCH(title) AGAINST(:title IN BOOLEAN MODE) AND removed_at IS NULL", nativeQuery = true)
    List<Post> findByTitleContaining(@Param("title") String title);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT p FROM Post p WHERE p.postType = :postType AND p.removedAt IS NULL")
    Page<Post> findByPostTypeWithUser(@Param("postType") PostType postType, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT p FROM Post p WHERE (p.postType = 'ADOPTION' OR p.postType = 'FOSTERING') AND p.removedAt IS NULL")
    Page<Post> findWithUser(Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT p FROM Post p WHERE p.parent.id = :parentId AND p.removedAt IS NULL")
    List<Post> findByParentIdWithUser(@Param("parentId") Long parentId);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT p FROM Post p WHERE p.id = :postId AND p.removedAt IS NULL")
    Optional<Post> findByIdWithUser(@Param("postId") Long postId);

    @EntityGraph(attributePaths = {"user", "parent"})
    @Query("SELECT p FROM Post p WHERE p.id = :postId AND p.removedAt IS NULL")
    Optional<Post> findByIdWithUserAndParent(@Param("postId") Long postId);

    @Query("SELECT prs.post.id FROM PostReadStatus prs WHERE prs.user.id = :userId")
    List<Long> findAllPostIdByUserId(@Param("userId") Long userId);

    @Query("SELECT p.id FROM Post p WHERE p.removedAt IS NULL AND p.createdDate > :date")
    List<Long> findPostIdsWithinDate(LocalDateTime date);

    @Query("SELECT COUNT(p) > 0 FROM Post p WHERE p.id = :id AND p.removedAt IS NULL")
    boolean existsById(Long id);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.createdDate >= :date AND p.removedAt IS NULL")
    Long countALlWithinDate(LocalDateTime date);

    @Modifying
    @Query("UPDATE Post p SET p.likeNum = :likeNum WHERE p.id = :postId AND p.removedAt IS NULL")
    void updateLikeNum(@Param("likeNum") Long likeNum, @Param("postId") Long postId);

    @Modifying
    @Query("UPDATE Post p SET p.answerNum = p.answerNum + 1 WHERE p.id = :postId")
    void incrementAnswerNum(@Param("postId") Long postId);

    @Modifying
    @Query("UPDATE Post p SET p.answerNum = p.answerNum - 1 WHERE p.id = :postId AND p.answerNum > 0")
    void decrementAnswerNum(@Param("postId") Long postId);

    @Modifying
    @Query("UPDATE Post p SET p.commentNum = p.commentNum + 1 WHERE p.id = :postId")
    void incrementCommentNum(@Param("postId") Long postId);

    @Modifying
    @Query("UPDATE Post p SET p.commentNum = p.commentNum - :decrementNum WHERE p.id = :postId AND p.commentNum > 0")
    void decrementCommentNum(@Param("postId") Long postId, @Param("decrementNum") Long decrementNum);

    void deleteAllByGroupId(Long groupId);
}