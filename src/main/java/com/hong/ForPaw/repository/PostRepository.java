package com.hong.ForPaw.repository;

import com.hong.ForPaw.domain.Post.Post;
import com.hong.ForPaw.domain.Post.Type;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByType(Type type, Pageable pageable);
}
