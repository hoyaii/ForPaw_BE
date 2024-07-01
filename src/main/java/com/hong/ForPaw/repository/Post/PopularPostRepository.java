package com.hong.ForPaw.repository.Post;

import com.hong.ForPaw.domain.Post.PopularPost;
import com.hong.ForPaw.domain.Post.PostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PopularPostRepository extends JpaRepository<PopularPost, Long> {

    @EntityGraph(attributePaths = {"post"})
    @Query("SELECT p FROM PopularPost p WHERE p.postType = :postType")
    Page<PopularPost> findAllWithPost(PostType postType, Pageable pageable);
}
