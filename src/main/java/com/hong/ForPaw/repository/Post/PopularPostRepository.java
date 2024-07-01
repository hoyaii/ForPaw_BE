package com.hong.ForPaw.repository.Post;

import com.hong.ForPaw.domain.Post.PopularPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PopularPostRepository extends JpaRepository<PopularPost, Long> {
}
