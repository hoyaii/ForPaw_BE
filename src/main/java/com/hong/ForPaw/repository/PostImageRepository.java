package com.hong.ForPaw.repository;

import com.hong.ForPaw.domain.Post.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostImageRepository extends JpaRepository<PostImage, Long> {

}
