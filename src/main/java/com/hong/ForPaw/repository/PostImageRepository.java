package com.hong.ForPaw.repository;

import com.hong.ForPaw.domain.Post.Post;
import com.hong.ForPaw.domain.Post.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    List<PostImage> findByPost(Post post);
}
