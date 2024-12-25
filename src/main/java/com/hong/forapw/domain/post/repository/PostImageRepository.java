package com.hong.forapw.domain.post.repository;

import com.hong.forapw.domain.post.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    @Modifying
    @Query("DELETE PostImage pi WHERE pi.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);

    @Modifying
    @Query("DELETE FROM PostImage pi " +
            "WHERE pi.post.id IN (SELECT p.id FROM Post p JOIN p.group g WHERE g.id = :groupId)")
    void deleteByGroupId(@Param("groupId") Long groupId);

    void deleteByPostIdAndIdNotIn(Long postId, List<Long> retainedImageIds);
}