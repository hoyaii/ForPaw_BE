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

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    boolean existsById(Long id);

    @Query("SELECT p.user.id FROM Post p WHERE p.id = :postId")
    Optional<Long> findUserIdByPostId(@Param("postId") Long postId);

    void deleteAllByGroupId(Long groupId);

    @EntityGraph(attributePaths = {"user"})
    Page<Post> findByGroupId(Long groupId, Pageable pageable);

    @EntityGraph(attributePaths = {"postImages"})
    Page<Post> findByTitleContaining(@Param("title") String title, Pageable pageable);

    @EntityGraph(attributePaths = {"postImages", "user"})
    Page<Post> findByPostType(PostType postType, Pageable pageable);

    @Query("SELECT p FROM Post p JOIN FETCH p.user WHERE p.parent.id = :parentId")
    List<Post> findByParentIdWithUser(@Param("parentId") Long parentId);

    @Query("SELECT p FROM Post p JOIN FETCH p.user WHERE p.id = :postId")
    Optional<Post> findByIdWithUser(@Param("postId") Long postId);

    @Query("SELECT prs.post.id FROM PostReadStatus prs WHERE prs.user.id = :userId")
    List<Long> findPostIdsByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Post p SET p.likeNum = :likeNum WHERE p.id = :postId")
    void updateLikeNum(@Param("likeNum") Long likeNum, @Param("postId") Long postId);

    @Query("SELECT p.id FROM Post p")
    Page<Long> findPostIds(Pageable pageable);
}