package com.hong.ForPaw.repository;

import com.hong.ForPaw.domain.Chat.ChatUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatUserRepository extends JpaRepository<ChatUser, Long> {
    
}
