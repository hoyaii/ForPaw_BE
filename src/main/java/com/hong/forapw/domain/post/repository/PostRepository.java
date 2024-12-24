package com.hong.forapw.domain.post.repository;

import com.hong.forapw.domain.user.model.PostTypeCountDTO;
import com.hong.forapw.domain.post.entity.Post;
import com.hong.forapw.domain.post.constant.PostType;
import com.hong.forapw.domain.user.entity.User;
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
public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p WHERE p.removedAt IS NULL")
    List<Post> findAll();

    @Query("SELECT p FROM Post p WHERE p.createdDate BETWEEN :startOfDay AND :endOfDay AND p.postType = :postType AND p.removedAt IS NULL")
    List<Post> findByDateAndType(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay, @Param("postType") PostType postType);

    @Query("SELECT p FROM Post p WHERE p.id = :id AND p.removedAt IS NULL")
    Optional<Post> findById(@Param("id") Long id);

    @Query("SELECT u FROM Post p " +
            "JOIN p.user u " +
            "WHERE p.id = :postId AND p.removedAt IS NULL")
    Optional<User> findUserById(@Param("postId") Long postId);

    @Query("SELECT u.id FROM Post p " +
            "JOIN p.user u " +
            "WHERE p.id = :postId AND p.removedAt IS NULL")
    Optional<Long> findUserIdById(@Param("postId") Long postId);

    @Query("SELECT p.postType FROM Post p WHERE p.id = :postId AND p.removedAt IS NULL")
    Optional<PostType> findPostTypeById(@Param("postId") Long postId);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT p FROM Post p " +
            "JOIN p.group g " +
            "WHERE p.postType = 'NOTICE' AND g.id = :groupId AND p.removedAt IS NULL")
    Page<Post> findNoticeByGroupIdWithUser(@Param("groupId") Long groupId, Pageable pageable);

    @Query(value = "SELECT p.id as postId, p.title, p.content, p.created_date as createdDate, " +
            "p.post_type as postType, MIN(pi.imageurl) as imageUrl, " +
            "u.id as userId, u.nick_name as nickName, p.comment_num as commentNum " +
            "FROM post_tb p " +
            "JOIN user_tb u ON p.user_id = u.id " +
            "LEFT JOIN post_image_tb pi ON pi.post_id = p.id " +
            "WHERE MATCH(p.title) AGAINST(:title IN BOOLEAN MODE) " +
            "AND p.post_type IN ('ADOPTION', 'FOSTERING', 'QUESTION') " +
            "AND p.removed_at IS NULL " +
            "GROUP BY p.id",
            countQuery = "SELECT COUNT(DISTINCT p.id) " +
                    "FROM post_tb p " +
                    "JOIN user_tb u ON p.user_id = u.id " +
                    "LEFT JOIN post_image_tb pi ON pi.post_id = p.id " +
                    "WHERE MATCH(p.title) AGAINST(:title IN BOOLEAN MODE) " +
                    "AND p.post_type IN ('ADOPTION', 'FOSTERING', 'QUESTION') " +
                    "AND p.removed_at IS NULL",
            nativeQuery = true)
    Page<Object[]> findByTitleContaining(@Param("title") String title, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT p FROM Post p WHERE p.postType = :postType AND p.removedAt IS NULL")
    Page<Post> findByPostTypeWithUser(@Param("postType") PostType postType, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT p FROM Post p " +
            "JOIN p.parent pr " +
            "WHERE pr.id = :parentId AND p.removedAt IS NULL")
    List<Post> findByParentIdWithUser(@Param("parentId") Long parentId);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT p FROM Post p " +
            "JOIN p.user u " +
            "WHERE u.id = :userId AND p.postType IN :postTypes AND p.removedAt IS NULL")
    Page<Post> findPostsByUserIdAndTypesWithUser(@Param("userId") Long userId, @Param("postTypes") List<PostType> postTypes, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT p.parent FROM Post p " +
            "JOIN p.user u " +
            "JOIN p.parent pr " +
            "WHERE u.id = :userId AND p.postType = 'ANSWER' AND pr.postType = 'QUESTION' AND pr.removedAt IS NULL")
    Page<Post> findQnaOfAnswerByUserIdWithUser(@Param("userId") Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT p FROM Post p WHERE p.id = :postId AND p.removedAt IS NULL")
    Optional<Post> findByIdWithUser(@Param("postId") Long postId);

    @EntityGraph(attributePaths = {"user", "parent"})
    @Query("SELECT p FROM Post p WHERE p.id = :postId AND p.removedAt IS NULL")
    Optional<Post> findByIdWithUserAndParent(@Param("postId") Long postId);

    @Query("SELECT p FROM Post p WHERE p.createdDate > :date AND p.removedAt IS NULL")
    List<Post> findPostIdsWithinDate(LocalDateTime date);

    @Query("SELECT COUNT(p) > 0 FROM Post p WHERE p.id = :id AND p.removedAt IS NULL")
    boolean existsById(Long id);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.createdDate >= :date AND p.removedAt IS NULL")
    Long countALlWithinDate(LocalDateTime date);

    @Query("SELECT new com.hong.forapw.controller.dto.query.PostTypeCountDTO(p.postType, COUNT(p)) " +
            "FROM Post p " +
            "WHERE p.user.id = :userId AND p.removedAt IS NULL AND p.postType IN :postTypes GROUP BY p.postType")
    List<PostTypeCountDTO> countByUserIdAndType(@Param("userId") Long userId, @Param("postTypes") List<PostType> postTypes);

    @Query("SELECT COUNT(pl) FROM PostLike pl WHERE pl.post.id = :postId")
    Long countLikesByPostId(@Param("postId") Long postId);

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


    @Modifying
    @Query("DELETE FROM Post p WHERE p.group.id = :groupId")
    void hardDeleteByGroupId(@Param("groupId") Long groupId);

    @Modifying
    @Query("DELETE FROM Post p WHERE p.id = :postId AND p.removedAt IS NULL")
    void deleteById(@Param("postId") Long postId);
}