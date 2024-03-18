package com.hong.ForPaw.repository.Chat;

import com.hong.ForPaw.domain.Chat.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

}