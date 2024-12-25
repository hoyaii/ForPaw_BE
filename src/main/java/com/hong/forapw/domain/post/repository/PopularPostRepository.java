package com.hong.forapw.domain.post.repository;

import com.hong.forapw.domain.post.entity.PopularPost;
import com.hong.forapw.domain.post.constant.PostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PopularPostRepository extends JpaRepository<PopularPost, Long> {

    @EntityGraph(attributePaths = {"post"})
    @Query("SELECT p FROM PopularPost p WHERE p.postType = :postType")
    Page<PopularPost> findByPostTypeWithPost(PostType postType, Pageable pageable);

    @EntityGraph(attributePaths = {"post"})
    @Query("SELECT p FROM PopularPost p")
    Page<PopularPost> findAllWithPost(Pageable pageable);

    @Modifying
    @Query("DELETE FROM PopularPost p WHERE p.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);
}
