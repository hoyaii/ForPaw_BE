package com.hong.ForPaw.repository;

import com.hong.ForPaw.domain.Post.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    
}
