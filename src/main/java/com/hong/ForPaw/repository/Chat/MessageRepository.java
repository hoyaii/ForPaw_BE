package com.hong.ForPaw.repository.Chat;

import com.hong.ForPaw.domain.Chat.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {

    Page<Message> findByChatRoomId(Long chatRoomId, Pageable pageable);

    Integer countByChatRoomId(Long chatRoomId);
}
