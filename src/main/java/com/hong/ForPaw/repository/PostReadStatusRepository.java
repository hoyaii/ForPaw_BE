package com.hong.ForPaw.repository;

import com.hong.ForPaw.domain.Post.PostReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostReadStatusRepository extends JpaRepository<PostReadStatus, Long> {
    
}
