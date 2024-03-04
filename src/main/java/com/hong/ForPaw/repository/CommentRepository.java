package com.hong.ForPaw.repository;

import com.hong.ForPaw.domain.Post.Comment;
import com.hong.ForPaw.domain.Post.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPost(Post post);
}
